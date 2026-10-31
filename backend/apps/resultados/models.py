"""Modelos del dominio Resultados (RF-13, RF-14, RF-26, RF-27) y registro de notificaciones."""

from django.conf import settings
from django.db import models

from apps.academico.models import Estudiante
from apps.participaciones.models import Participacion


class Resultado(models.Model):
    """Calificación y retroalimentación final de un intento de participación."""

    participacion = models.OneToOneField(
        Participacion, on_delete=models.CASCADE, related_name='resultado',
    )
    correctas = models.PositiveIntegerField('respuestas correctas', default=0)
    incorrectas = models.PositiveIntegerField('respuestas incorrectas', default=0)
    no_respondidas = models.PositiveIntegerField('preguntas sin responder', default=0)
    peso_obtenido = models.PositiveIntegerField('peso obtenido', default=0)
    peso_total = models.PositiveIntegerField('peso total del caso', default=0)
    nota_final = models.DecimalField('nota final', max_digits=6, decimal_places=2, default=0)
    feedback_docente = models.TextField('feedback general del docente', blank=True)
    docente_feedback = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        verbose_name='docente que registra la retroalimentación',
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name='retroalimentaciones_registradas',
    )
    fecha_feedback = models.DateTimeField('fecha de retroalimentación', null=True, blank=True)
    notificado_estudiante = models.BooleanField('notificado al estudiante', default=False)
    fecha_calculo = models.DateTimeField('fecha del cálculo', auto_now_add=True)
    fecha_actualizacion = models.DateTimeField(auto_now=True)

    class Meta:
        verbose_name = 'Resultado'
        verbose_name_plural = 'Resultados'
        ordering = ['-fecha_calculo']

    def __str__(self) -> str:
        return f'Resultado de {self.participacion.estudiante.correo}: {self.nota_final}'


class NotificacionEnvio(models.Model):
    """Historial de envíos de correo (RF-21, RF-27). Evita duplicados y registra errores."""

    class Tipo(models.TextChoices):
        CONVOCATORIA = 'CONVOCATORIA', 'Convocatoria'
        CAMBIO_CONVOCATORIA = 'CAMBIO_CONVOCATORIA', 'Cambio de convocatoria'
        CALIFICACION = 'CALIFICACION', 'Calificación'
        RETROALIMENTACION = 'RETROALIMENTACION', 'Retroalimentación'

    class Estado(models.TextChoices):
        PENDIENTE = 'PENDIENTE', 'Pendiente'
        ENVIADO = 'ENVIADO', 'Enviado'
        ERROR = 'ERROR', 'Error'

    tipo = models.CharField(max_length=32, choices=Tipo.choices)
    event_key = models.CharField('clave anti-duplicado', max_length=191, unique=True)
    estudiante = models.ForeignKey(
        Estudiante, on_delete=models.CASCADE, related_name='notificaciones_envio',
    )
    destinatario = models.EmailField()
    asunto = models.CharField(max_length=200)
    cuerpo = models.TextField()
    estado = models.CharField(max_length=16, choices=Estado.choices, default=Estado.PENDIENTE)
    error_mensaje = models.TextField(blank=True)
    practica_id = models.IntegerField(null=True, blank=True)
    resultado_id = models.IntegerField(null=True, blank=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)
    fecha_envio = models.DateTimeField(null=True, blank=True)

    class Meta:
        verbose_name = 'Envío de notificación'
        verbose_name_plural = 'Envíos de notificación'
        ordering = ['-fecha_creacion']

    def __str__(self) -> str:
        return f'{self.tipo} → {self.destinatario} [{self.estado}]'
