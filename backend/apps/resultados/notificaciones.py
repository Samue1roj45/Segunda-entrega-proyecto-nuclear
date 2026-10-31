"""Facade de notificaciones por correo Django (RF-21, RF-27)."""

from __future__ import annotations

import hashlib
import logging

from django.conf import settings
from django.core.mail import send_mail
from django.db import transaction
from django.utils import timezone

from apps.academico.models import Estudiante
from apps.practicas.models import AutorizacionEstudiante, Practica

from .events import Evento
from .models import NotificacionEnvio, Resultado

logger = logging.getLogger(__name__)


class NotificacionFacade:
    def enviar_si_nuevo(
        self,
        *,
        tipo: str,
        event_key: str,
        estudiante: Estudiante,
        asunto: str,
        cuerpo: str,
        practica_id: int | None = None,
        resultado_id: int | None = None,
    ) -> NotificacionEnvio | None:
        with transaction.atomic():
            existente = NotificacionEnvio.objects.filter(event_key=event_key).first()
            if existente and existente.estado == NotificacionEnvio.Estado.ENVIADO:
                return existente
            registro, _ = NotificacionEnvio.objects.update_or_create(
                event_key=event_key,
                defaults={
                    'tipo': tipo,
                    'estudiante': estudiante,
                    'destinatario': estudiante.correo,
                    'asunto': asunto,
                    'cuerpo': cuerpo,
                    'estado': NotificacionEnvio.Estado.PENDIENTE,
                    'error_mensaje': '',
                    'practica_id': practica_id,
                    'resultado_id': resultado_id,
                },
            )
        try:
            send_mail(
                asunto,
                cuerpo,
                settings.DEFAULT_FROM_EMAIL,
                [estudiante.correo],
                fail_silently=False,
            )
        except Exception as exc:
            logger.exception('Fallo al enviar notificación %s', event_key)
            registro.estado = NotificacionEnvio.Estado.ERROR
            registro.error_mensaje = str(exc)
            registro.save(update_fields=['estado', 'error_mensaje'])
            return registro

        registro.estado = NotificacionEnvio.Estado.ENVIADO
        registro.fecha_envio = timezone.now()
        registro.error_mensaje = ''
        registro.save(update_fields=['estado', 'fecha_envio', 'error_mensaje'])
        return registro

    def convocatoria_creada(self, autorizacion: AutorizacionEstudiante) -> None:
        practica = autorizacion.practica
        estudiante = autorizacion.estudiante
        cuerpo = (
            f'Hola {estudiante.nombre_completo},\n\n'
            f'Has sido autorizado/a para participar en la práctica académica:\n'
            f'  • {practica.nombre}\n'
            f'  • Caso: {practica.caso.nombre}\n'
            f'  • Inicio: {practica.fecha_inicio:%Y-%m-%d %H:%M}\n'
            f'  • Fin:    {practica.fecha_fin:%Y-%m-%d %H:%M}\n\n'
            f'Tu código de acceso es: {autorizacion.codigo_acceso}\n\n'
            f'Ingresa al simulador con tu correo y este código.\n\n'
            f'(Mensaje del docente: {practica.mensaje_personalizado or "-"})\n'
        )
        envio = self.enviar_si_nuevo(
            tipo=NotificacionEnvio.Tipo.CONVOCATORIA,
            event_key=f'convocatoria:{autorizacion.id}',
            estudiante=estudiante,
            asunto=f'Acceso a la práctica: {practica.nombre}',
            cuerpo=cuerpo,
            practica_id=practica.id,
        )
        if envio and envio.estado == NotificacionEnvio.Estado.ENVIADO and not autorizacion.notificado:
            autorizacion.notificado = True
            autorizacion.save(update_fields=['notificado'])

    def convocatoria_modificada(self, practica: Practica) -> None:
        version = practica.fecha_actualizacion.isoformat() if practica.fecha_actualizacion else 'na'
        for autorizacion in practica.autorizaciones.select_related('estudiante', 'practica__caso'):
            estudiante = autorizacion.estudiante
            cuerpo = (
                f'Hola {estudiante.nombre_completo},\n\n'
                f'Hubo cambios en la convocatoria de la práctica "{practica.nombre}".\n'
                f'  • Caso: {practica.caso.nombre}\n'
                f'  • Inicio: {practica.fecha_inicio:%Y-%m-%d %H:%M}\n'
                f'  • Fin: {practica.fecha_fin:%Y-%m-%d %H:%M}\n'
                f'  • Estado: {practica.get_estado_display()}\n'
                f'  • Mensaje: {practica.mensaje_personalizado or "-"}\n\n'
                f'Tu código de acceso sigue siendo: {autorizacion.codigo_acceso}\n'
            )
            self.enviar_si_nuevo(
                tipo=NotificacionEnvio.Tipo.CAMBIO_CONVOCATORIA,
                event_key=f'cambio:{autorizacion.id}:{version}',
                estudiante=estudiante,
                asunto=f'Cambios en la práctica: {practica.nombre}',
                cuerpo=cuerpo,
                practica_id=practica.id,
            )

    def calificacion_publicada(self, resultado: Resultado) -> None:
        participacion = resultado.participacion
        estudiante = participacion.estudiante
        practica = participacion.practica
        cuerpo = (
            f'Hola {estudiante.nombre_completo},\n\n'
            f'Tu calificación de la práctica "{practica.nombre}" (intento {participacion.numero_intento}) '
            f'ya está disponible.\n'
            f'Ingresa al simulador para consultar el detalle.\n'
        )
        envio = self.enviar_si_nuevo(
            tipo=NotificacionEnvio.Tipo.CALIFICACION,
            event_key=f'calificacion:{resultado.id}',
            estudiante=estudiante,
            asunto=f'Calificación publicada: {practica.nombre}',
            cuerpo=cuerpo,
            practica_id=practica.id,
            resultado_id=resultado.id,
        )
        if envio and envio.estado == NotificacionEnvio.Estado.ENVIADO:
            resultado.notificado_estudiante = True
            resultado.save(update_fields=['notificado_estudiante'])

    def retroalimentacion_publicada(self, resultado: Resultado) -> None:
        participacion = resultado.participacion
        estudiante = participacion.estudiante
        practica = participacion.practica
        huella = hashlib.sha256((resultado.feedback_docente or '').encode('utf-8')).hexdigest()[:16]
        cuerpo = (
            f'Hola {estudiante.nombre_completo},\n\n'
            f'El docente registró retroalimentación en la práctica "{practica.nombre}" '
            f'(intento {participacion.numero_intento}).\n'
            f'Ingresa al simulador para leer el comentario. Este mensaje no incluye el texto completo.\n'
        )
        self.enviar_si_nuevo(
            tipo=NotificacionEnvio.Tipo.RETROALIMENTACION,
            event_key=f'feedback:{resultado.id}:{huella}',
            estudiante=estudiante,
            asunto=f'Retroalimentación disponible: {practica.nombre}',
            cuerpo=cuerpo,
            practica_id=practica.id,
            resultado_id=resultado.id,
        )


class ObservadorConvocatoria:
    def actualizar(self, evento: Evento) -> None:
        facade = NotificacionFacade()
        if evento.nombre == 'convocatoria_creada':
            facade.convocatoria_creada(evento.payload['autorizacion'])
        elif evento.nombre == 'convocatoria_modificada':
            facade.convocatoria_modificada(evento.payload['practica'])


class ObservadorCalificacion:
    def actualizar(self, evento: Evento) -> None:
        NotificacionFacade().calificacion_publicada(evento.payload['resultado'])


class ObservadorRetroalimentacion:
    def actualizar(self, evento: Evento) -> None:
        NotificacionFacade().retroalimentacion_publicada(evento.payload['resultado'])


_OBS_CONVOCATORIA = ObservadorConvocatoria()
_OBS_CALIFICACION = ObservadorCalificacion()
_OBS_RETRO = ObservadorRetroalimentacion()


def registrar_observadores() -> None:
    from .events import (
        CALIFICACION_PUBLICADA,
        CONVOCATORIA_CREADA,
        CONVOCATORIA_MODIFICADA,
        RETROALIMENTACION_PUBLICADA,
        publicador,
    )

    publicador.suscribir(CONVOCATORIA_CREADA, _OBS_CONVOCATORIA)
    publicador.suscribir(CONVOCATORIA_MODIFICADA, _OBS_CONVOCATORIA)
    publicador.suscribir(CALIFICACION_PUBLICADA, _OBS_CALIFICACION)
    publicador.suscribir(RETROALIMENTACION_PUBLICADA, _OBS_RETRO)
