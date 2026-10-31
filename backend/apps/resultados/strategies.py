"""Patrón Strategy: reglas de evaluación (RF-13, RF-24) sin alterar la fórmula existente."""

from __future__ import annotations

from decimal import Decimal
from typing import Protocol

from apps.casos.models import Pregunta
from apps.participaciones.models import Participacion, RespuestaSeleccionada

from .models import Resultado


def escala_del_caso(caso) -> int:
    """Escala máxima de la rúbrica del caso; 100 si no hay rúbrica (comportamiento actual)."""
    try:
        return int(caso.rubrica.escala_maxima)
    except Exception:
        return 100


def metricas_por_respuestas(participacion: Participacion) -> dict:
    """
    Fórmula vigente:
    nota = (peso_obtenido / peso_total) * rubrica.escala_maxima
    Preguntas sin respuesta no suman a peso_obtenido.
    """
    caso = participacion.practica.caso
    preguntas = list(Pregunta.objects.filter(escenario__caso=caso))
    seleccionadas = {
        rs.pregunta_id: rs
        for rs in RespuestaSeleccionada.objects.filter(
            participacion=participacion,
        ).select_related('respuesta_elegida')
    }

    correctas = 0
    incorrectas = 0
    no_respondidas = 0
    peso_obtenido = 0
    peso_total = sum(p.peso for p in preguntas)

    for pregunta in preguntas:
        rs = seleccionadas.get(pregunta.id)
        if rs is None:
            no_respondidas += 1
            continue
        if rs.respuesta_elegida.es_correcta:
            correctas += 1
            peso_obtenido += pregunta.peso
        else:
            incorrectas += 1

    escala = escala_del_caso(caso)
    if peso_total > 0:
        nota = (Decimal(peso_obtenido) / Decimal(peso_total) * Decimal(escala)).quantize(Decimal('0.01'))
    else:
        nota = Decimal('0.00')

    return {
        'correctas': correctas,
        'incorrectas': incorrectas,
        'no_respondidas': no_respondidas,
        'peso_obtenido': peso_obtenido,
        'peso_total': peso_total,
        'nota_final': nota,
    }


class EstrategiaEvaluacion(Protocol):
    def calcular(self, participacion: Participacion) -> dict: ...


class EvaluacionCompletaStrategy:
    """Participación FINALIZADA: califica el intento completo con la fórmula del proyecto."""

    def calcular(self, participacion: Participacion) -> dict:
        return metricas_por_respuestas(participacion)


class EvaluacionIncompletaStrategy:
    """RN21: califica hasta donde avanzó; no respondidas no aportan peso obtenido."""

    def calcular(self, participacion: Participacion) -> dict:
        return metricas_por_respuestas(participacion)


class EvaluacionAusenciaStrategy:
    """RN22: autorizado que nunca inició → nota 0."""

    def calcular(self, participacion: Participacion) -> dict:
        caso = participacion.practica.caso
        preguntas = list(Pregunta.objects.filter(escenario__caso=caso))
        peso_total = sum(p.peso for p in preguntas)
        return {
            'correctas': 0,
            'incorrectas': 0,
            'no_respondidas': len(preguntas),
            'peso_obtenido': 0,
            'peso_total': peso_total,
            'nota_final': Decimal('0.00'),
        }


def estrategia_para(participacion: Participacion) -> EstrategiaEvaluacion:
    if participacion.estado == Participacion.Estado.FINALIZADA:
        return EvaluacionCompletaStrategy()
    if participacion.estado == Participacion.Estado.INCOMPLETA:
        return EvaluacionIncompletaStrategy()
    return EvaluacionAusenciaStrategy()


def persistir_resultado(participacion: Participacion, metricas: dict) -> Resultado:
    resultado, _ = Resultado.objects.update_or_create(
        participacion=participacion,
        defaults=metricas,
    )
    return resultado
