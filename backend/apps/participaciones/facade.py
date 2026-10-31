"""Facade de participaciones: inicio, fin, listado e historial de intentos (RF-12, RF-15)."""

from __future__ import annotations

from django.db import transaction
from django.db.models import Max
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.exceptions import PermissionDenied, ValidationError

from apps.practicas.models import AutorizacionEstudiante, Practica
from apps.resultados.facade import EvaluacionFacade
from apps.resultados.proxy import AccesoEvaluacionProxy
from apps.usuarios.models import Usuario

from .models import Participacion, RespuestaSeleccionada
from .rn17 import validar_ventana_inicio
from .state import maquina_estado


class ParticipacionFacade:
    def __init__(self) -> None:
        self.proxy = AccesoEvaluacionProxy()
        self.evaluacion = EvaluacionFacade()

    def listar(self, user, filtros: dict | None = None):
        qs = self.proxy.participaciones_visibles(user)
        filtros = filtros or {}
        if filtros.get('practica'):
            qs = qs.filter(practica_id=filtros['practica'])
        if filtros.get('estudiante'):
            qs = qs.filter(estudiante_id=filtros['estudiante'])
        if filtros.get('estado'):
            qs = qs.filter(estado=filtros['estado'])
        if filtros.get('numero_intento'):
            qs = qs.filter(numero_intento=filtros['numero_intento'])
        if filtros.get('fecha_desde'):
            qs = qs.filter(inicio__date__gte=filtros['fecha_desde'])
        if filtros.get('fecha_hasta'):
            qs = qs.filter(inicio__date__lte=filtros['fecha_hasta'])
        return qs

    def obtener(self, user, pk: int) -> Participacion:
        return self.proxy.asegurar_participacion(user, pk)

    @transaction.atomic
    def iniciar(self, user, autorizacion_id: int) -> Participacion:
        if not user.is_authenticated or user.rol != Usuario.Rol.ESTUDIANTE:
            raise PermissionDenied('Solo estudiantes pueden iniciar participaciones.')
        if not autorizacion_id:
            raise ValidationError('El token no contiene autorizacion_id; vuelve a acceder con tu código.')

        autorizacion = get_object_or_404(
            AutorizacionEstudiante.objects.select_for_update().select_related(
                'practica__caso', 'estudiante',
            ),
            pk=autorizacion_id,
        )
        if autorizacion.estudiante.usuario_id != user.id:
            raise PermissionDenied('La autorización no corresponde al estudiante autenticado.')

        practica = autorizacion.practica
        ahora = timezone.now()
        if practica.estado == Practica.Estado.CANCELADA:
            raise ValidationError('La práctica fue cancelada.')
        if practica.estado == Practica.Estado.FINALIZADA or ahora > practica.fecha_fin:
            raise ValidationError('La práctica ya finalizó.')

        existentes = list(
            Participacion.objects.select_for_update().filter(autorizacion=autorizacion).order_by('numero_intento')
        )
        en_curso = next((p for p in existentes if p.estado == Participacion.Estado.EN_CURSO), None)
        if en_curso:
            return en_curso

        no_iniciada = next((p for p in existentes if p.estado == Participacion.Estado.NO_INICIADA), None)
        terminales = [p for p in existentes if p.es_terminal]

        if terminales and not autorizacion.reintento_autorizado:
            raise ValidationError('Ya finalizaste esta práctica.')

        validar_ventana_inicio(practica, ahora)

        if no_iniciada and not terminales:
            return maquina_estado(no_iniciada).iniciar(no_iniciada, ahora)

        siguiente = (Participacion.objects.filter(autorizacion=autorizacion).aggregate(
            m=Max('numero_intento'),
        )['m'] or 0) + 1

        participacion = Participacion.objects.create(
            practica=practica,
            estudiante=autorizacion.estudiante,
            autorizacion=autorizacion,
            numero_intento=siguiente,
            estado=Participacion.Estado.NO_INICIADA,
        )
        maquina_estado(participacion).iniciar(participacion, ahora)

        if terminales and autorizacion.reintento_autorizado:
            autorizacion.reintento_autorizado = False
            autorizacion.save(update_fields=['reintento_autorizado'])

        return participacion

    @transaction.atomic
    def responder(self, user, pk: int, pregunta, respuesta) -> Participacion:
        participacion = self.proxy.asegurar_participacion(user, pk)
        if participacion.es_terminal:
            raise ValidationError('No es posible alterar respuestas de un intento cerrado.')
        if participacion.estado != Participacion.Estado.EN_CURSO:
            raise ValidationError('La participación no está en curso.')
        RespuestaSeleccionada.objects.update_or_create(
            participacion=participacion,
            pregunta=pregunta,
            defaults={'respuesta_elegida': respuesta},
        )
        return participacion

    @transaction.atomic
    def finalizar(self, user, pk: int) -> tuple[Participacion, object]:
        participacion = self.proxy.asegurar_participacion(user, pk)
        if participacion.estado == Participacion.Estado.FINALIZADA and hasattr(participacion, 'resultado'):
            return participacion, participacion.resultado
        maquina_estado(participacion).finalizar(participacion)
        resultado = self.evaluacion.calcular(participacion)
        return participacion, resultado
