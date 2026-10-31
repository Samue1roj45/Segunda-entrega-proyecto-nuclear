"""Signals de prácticas: publican eventos de convocatoria (Observer en resultados)."""

from django.db.models.signals import post_save, pre_save
from django.dispatch import receiver

from apps.resultados.events import CONVOCATORIA_CREADA, CONVOCATORIA_MODIFICADA, Evento, publicador

from .models import AutorizacionEstudiante, Practica

_CAMPOS_CONVOCATORIA = (
    'nombre', 'fecha_inicio', 'fecha_fin', 'mensaje_personalizado',
    'estado', 'lugar_fisico',
)


@receiver(post_save, sender=AutorizacionEstudiante)
def publicar_convocatoria_creada(sender, instance: AutorizacionEstudiante, created: bool, **kwargs):
    if not created:
        return
    publicador.notificar(Evento(CONVOCATORIA_CREADA, {'autorizacion': instance}))


@receiver(pre_save, sender=Practica)
def recordar_practica_previa(sender, instance: Practica, **kwargs):
    if not instance.pk:
        instance._practica_previa = None
        return
    try:
        instance._practica_previa = Practica.objects.get(pk=instance.pk)
    except Practica.DoesNotExist:
        instance._practica_previa = None


@receiver(post_save, sender=Practica)
def publicar_convocatoria_modificada(sender, instance: Practica, created: bool, **kwargs):
    if created:
        return
    previa = getattr(instance, '_practica_previa', None)
    if previa is None:
        return
    if any(getattr(previa, campo) != getattr(instance, campo) for campo in _CAMPOS_CONVOCATORIA):
        publicador.notificar(Evento(CONVOCATORIA_MODIFICADA, {'practica': instance}))
