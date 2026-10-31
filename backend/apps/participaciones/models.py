"""Modelos del dominio Participaciones (RF30-RF36 / RF-12, RF-24)."""

from django.db import models

from apps.academico.models import Estudiante
from apps.casos.models import Pregunta, Respuesta
from apps.practicas.models import AutorizacionEstudiante, Practica


class Participacion(models.Model):
    """Participación de un estudiante en una práctica. Cada intento es un registro independiente."""

    class Estado(models.TextChoices):
        NO_INICIADA = 'NO_INICIADA', 'No iniciada'
        EN_CURSO = 'EN_CURSO', 'En curso'
        FINALIZADA = 'FINALIZADA', 'Finalizada'
        INCOMPLETA = 'INCOMPLETA', 'Incompleta'

    practica = models.ForeignKey(Practica, on_delete=models.CASCADE, related_name='participaciones')
    estudiante = models.ForeignKey(Estudiante, on_delete=models.CASCADE, related_name='participaciones')
    autorizacion = models.ForeignKey(
        AutorizacionEstudiante,
        on_delete=models.CASCADE,
        related_name='participaciones',
    )
    numero_intento = models.PositiveIntegerField('número de intento', default=1)
    inicio = models.DateTimeField('inicio', null=True, blank=True)
    fin = models.DateTimeField('fin', null=True, blank=True)
    tiempo_usado_seg = models.PositiveIntegerField('tiempo usado (segundos)', default=0)
    estado = models.CharField('estado', max_length=20, choices=Estado.choices, default=Estado.NO_INICIADA)

    class Meta:
        verbose_name = 'Participación'
        verbose_name_plural = 'Participaciones'
        ordering = ['-inicio', '-id']
        constraints = [
            models.UniqueConstraint(
                fields=['autorizacion', 'numero_intento'],
                name='unique_intento_por_autorizacion',
            ),
        ]

    def __str__(self) -> str:
        return f'{self.estudiante.correo} en {self.practica.nombre} (intento {self.numero_intento})'

    @property
    def es_terminal(self) -> bool:
        return self.estado in (self.Estado.FINALIZADA, self.Estado.INCOMPLETA)

    @property
    def es_consultable_resultado(self) -> bool:
        return self.es_terminal and hasattr(self, 'resultado')


class RespuestaSeleccionada(models.Model):
    """Respuesta elegida por el estudiante para una pregunta en un intento concreto."""

    participacion = models.ForeignKey(
        Participacion, on_delete=models.CASCADE, related_name='respuestas_seleccionadas',
    )
    pregunta = models.ForeignKey(Pregunta, on_delete=models.CASCADE)
    respuesta_elegida = models.ForeignKey(Respuesta, on_delete=models.CASCADE)
    timestamp = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = 'Respuesta seleccionada'
        verbose_name_plural = 'Respuestas seleccionadas'
        constraints = [
            models.UniqueConstraint(
                fields=['participacion', 'pregunta'],
                name='unique_respuesta_por_pregunta_participacion',
            ),
        ]
