"""Patrón State: transiciones válidas de una participación (RF-12, RF-14, RF-24)."""

from __future__ import annotations

from django.utils import timezone

from rest_framework.exceptions import ValidationError


class TransicionInvalida(ValidationError):
    """Transición de estado no permitida."""


class EstadoParticipacion:
    nombre: str

    def iniciar(self, participacion, ahora=None):
        raise TransicionInvalida('No es posible iniciar esta participación en su estado actual.')

    def finalizar(self, participacion, ahora=None):
        raise TransicionInvalida('No es posible finalizar una participación que no está en curso.')

    def marcar_incompleta(self, participacion, ahora=None):
        raise TransicionInvalida('Solo una participación en curso puede marcarse como incompleta.')


class EstadoNoIniciada(EstadoParticipacion):
    nombre = 'NO_INICIADA'

    def iniciar(self, participacion, ahora=None):
        momento = ahora or timezone.now()
        participacion.estado = participacion.Estado.EN_CURSO
        participacion.inicio = momento
        participacion.fin = None
        participacion.tiempo_usado_seg = 0
        participacion.save(update_fields=['estado', 'inicio', 'fin', 'tiempo_usado_seg'])
        return participacion


class EstadoEnCurso(EstadoParticipacion):
    nombre = 'EN_CURSO'

    def iniciar(self, participacion, ahora=None):
        return participacion

    def finalizar(self, participacion, ahora=None):
        momento = ahora or timezone.now()
        participacion.estado = participacion.Estado.FINALIZADA
        participacion.fin = momento
        if participacion.inicio:
            participacion.tiempo_usado_seg = max(0, int((momento - participacion.inicio).total_seconds()))
        participacion.save(update_fields=['estado', 'fin', 'tiempo_usado_seg'])
        return participacion

    def marcar_incompleta(self, participacion, ahora=None):
        momento = ahora or timezone.now()
        participacion.estado = participacion.Estado.INCOMPLETA
        participacion.fin = momento
        if participacion.inicio:
            participacion.tiempo_usado_seg = max(0, int((momento - participacion.inicio).total_seconds()))
        participacion.save(update_fields=['estado', 'fin', 'tiempo_usado_seg'])
        return participacion


class EstadoFinalizada(EstadoParticipacion):
    nombre = 'FINALIZADA'

    def iniciar(self, participacion, ahora=None):
        raise TransicionInvalida('Esta participación ya finalizó. Solicita un nuevo intento al docente.')

    def finalizar(self, participacion, ahora=None):
        return participacion


class EstadoIncompleta(EstadoParticipacion):
    nombre = 'INCOMPLETA'

    def iniciar(self, participacion, ahora=None):
        raise TransicionInvalida('Esta participación quedó incompleta. Solicita un nuevo intento al docente.')

    def finalizar(self, participacion, ahora=None):
        raise TransicionInvalida('Una participación incompleta no puede finalizarse de nuevo.')


_ESTADOS = {
    'NO_INICIADA': EstadoNoIniciada,
    'EN_CURSO': EstadoEnCurso,
    'FINALIZADA': EstadoFinalizada,
    'INCOMPLETA': EstadoIncompleta,
}


def maquina_estado(participacion) -> EstadoParticipacion:
    cls = _ESTADOS.get(participacion.estado)
    if cls is None:
        raise TransicionInvalida(f'Estado de participación desconocido: {participacion.estado}')
    return cls()
