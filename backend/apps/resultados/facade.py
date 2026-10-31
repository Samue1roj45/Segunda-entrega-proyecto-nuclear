"""Facades de evaluación, consulta, retroalimentación y pendientes (RF-13, 14, 24, 26)."""

from __future__ import annotations

from django.db import transaction
from django.utils import timezone
from rest_framework.exceptions import ValidationError

from apps.participaciones.models import Participacion
from apps.participaciones.state import maquina_estado
from apps.practicas.models import AutorizacionEstudiante, Practica

from .events import CALIFICACION_PUBLICADA, RETROALIMENTACION_PUBLICADA, Evento, publicador
from .models import Resultado
from .proxy import AccesoEvaluacionProxy
from .strategies import estrategia_para, persistir_resultado


class EvaluacionFacade:
    """Coordina respuestas, rúbrica, strategy y persistencia del resultado."""

    def calcular(self, participacion: Participacion, notificar: bool = True) -> Resultado:
        estrategia = estrategia_para(participacion)
        metricas = estrategia.calcular(participacion)
        creado = not Resultado.objects.filter(participacion=participacion).exists()
        resultado = persistir_resultado(participacion, metricas)
        if notificar and creado:
            publicador.notificar(Evento(CALIFICACION_PUBLICADA, {'resultado': resultado}))
        return resultado

    @transaction.atomic
    def evaluar_pendientes(self, practica: Practica) -> dict:
        """
        RF-24: distingue ausencia (nunca inició) e incompleta (inició y no terminó).
        Idempotente: no duplica resultados de ejecuciones previas.
        """
        resumen = {'incompletas': 0, 'ausencias': 0, 'omitidas': 0}
        autorizaciones = AutorizacionEstudiante.objects.filter(
            practica=practica,
        ).select_related('estudiante')

        for autorizacion in autorizaciones:
            participaciones = list(
                Participacion.objects.select_for_update().filter(autorizacion=autorizacion)
            )
            en_curso = [p for p in participaciones if p.estado == Participacion.Estado.EN_CURSO]
            if en_curso:
                for part in en_curso:
                    maquina_estado(part).marcar_incompleta(part)
                    self.calcular(part)
                    resumen['incompletas'] += 1
                continue

            if any(p.es_terminal for p in participaciones):
                resumen['omitidas'] += 1
                continue

            no_iniciadas = [p for p in participaciones if p.estado == Participacion.Estado.NO_INICIADA]
            if no_iniciadas:
                part = no_iniciadas[0]
            else:
                part = Participacion.objects.create(
                    practica=practica,
                    estudiante=autorizacion.estudiante,
                    autorizacion=autorizacion,
                    numero_intento=1,
                    estado=Participacion.Estado.NO_INICIADA,
                )
            if hasattr(part, 'resultado'):
                resumen['omitidas'] += 1
                continue
            self.calcular(part)
            resumen['ausencias'] += 1
        return resumen


class ConsultaResultadosFacade:
    def __init__(self) -> None:
        self.proxy = AccesoEvaluacionProxy()

    def listar(self, user):
        return self.proxy.resultados_visibles(user)

    def obtener(self, user, pk: int) -> Resultado:
        resultado = self.proxy.asegurar_resultado(user, pk)
        if not resultado.participacion.es_consultable_resultado:
            raise ValidationError('El resultado aún no está disponible para consulta.')
        return resultado


class RetroalimentacionFacade:
    def __init__(self) -> None:
        self.proxy = AccesoEvaluacionProxy()

    @transaction.atomic
    def registrar(self, user, resultado_id: int, texto: str) -> Resultado:
        resultado = self.proxy.asegurar_resultado(user, resultado_id)
        self.proxy.asegurar_docente_de_participacion(user, resultado.participacion)
        if not resultado.participacion.es_terminal:
            raise ValidationError('Solo se puede registrar retroalimentación sobre un intento cerrado.')
        resultado.feedback_docente = texto
        resultado.docente_feedback = user
        resultado.fecha_feedback = timezone.now()
        resultado.save(update_fields=[
            'feedback_docente', 'docente_feedback', 'fecha_feedback', 'fecha_actualizacion',
        ])
        publicador.notificar(Evento(RETROALIMENTACION_PUBLICADA, {'resultado': resultado}))
        return resultado
