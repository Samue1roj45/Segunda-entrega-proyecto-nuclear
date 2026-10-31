"""RN17: no iniciar si ya transcurrió la mitad del intervalo de la práctica."""

from datetime import datetime

from rest_framework.exceptions import ValidationError

from apps.practicas.models import Practica


def punto_medio_practica(practica: Practica) -> datetime:
    return practica.fecha_inicio + (practica.fecha_fin - practica.fecha_inicio) / 2


def validar_ventana_inicio(practica: Practica, ahora: datetime) -> None:
    """Rechaza un *nuevo* inicio si ya pasó el punto medio del intervalo [inicio, fin]."""
    limite = punto_medio_practica(practica)
    if ahora > limite:
        raise ValidationError(
            'No puedes iniciar la participación: ya transcurrió la mitad del tiempo '
            'establecido para la práctica.'
        )
