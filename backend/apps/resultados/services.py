"""Punto de compatibilidad: el cálculo vive en EvaluacionFacade + Strategy."""

from apps.participaciones.models import Participacion

from .facade import EvaluacionFacade
from .models import Resultado


def calcular_resultado(participacion: Participacion) -> Resultado:
    return EvaluacionFacade().calcular(participacion)
