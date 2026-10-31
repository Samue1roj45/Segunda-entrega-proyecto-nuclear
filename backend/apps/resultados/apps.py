from django.apps import AppConfig


class ResultadosConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'apps.resultados'
    verbose_name = 'Resultados y calificaciones'

    def ready(self):
        from .notificaciones import registrar_observadores
        registrar_observadores()
