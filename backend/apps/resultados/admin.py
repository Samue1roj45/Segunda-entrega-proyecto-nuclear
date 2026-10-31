from django.contrib import admin

from .models import NotificacionEnvio, Resultado


@admin.register(Resultado)
class ResultadoAdmin(admin.ModelAdmin):
    list_display = (
        'participacion', 'nota_final', 'correctas', 'incorrectas',
        'no_respondidas', 'docente_feedback', 'notificado_estudiante',
    )
    list_filter = ('notificado_estudiante',)
    search_fields = ('participacion__estudiante__correo', 'participacion__practica__nombre')
    readonly_fields = (
        'participacion', 'correctas', 'incorrectas', 'no_respondidas',
        'peso_obtenido', 'peso_total', 'nota_final', 'fecha_calculo', 'fecha_actualizacion',
    )


@admin.register(NotificacionEnvio)
class NotificacionEnvioAdmin(admin.ModelAdmin):
    list_display = ('tipo', 'destinatario', 'estado', 'event_key', 'fecha_creacion')
    list_filter = ('tipo', 'estado')
    search_fields = ('destinatario', 'event_key', 'asunto')
    readonly_fields = ('event_key', 'fecha_creacion', 'fecha_envio')
