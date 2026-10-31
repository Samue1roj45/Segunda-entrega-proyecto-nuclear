"""Patrón Observer: publicador de eventos de evaluación y convocatoria."""

from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field
from typing import Any, Protocol


@dataclass(frozen=True)
class Evento:
    nombre: str
    payload: dict[str, Any] = field(default_factory=dict)


class Observador(Protocol):
    def actualizar(self, evento: Evento) -> None: ...


class PublicadorEventos:
    def __init__(self) -> None:
        self._suscriptores: dict[str, list[Observador]] = defaultdict(list)

    def suscribir(self, nombre: str, observador: Observador) -> None:
        if observador not in self._suscriptores[nombre]:
            self._suscriptores[nombre].append(observador)

    def notificar(self, evento: Evento) -> None:
        for observador in list(self._suscriptores.get(evento.nombre, [])):
            observador.actualizar(evento)


publicador = PublicadorEventos()

CONVOCATORIA_CREADA = 'convocatoria_creada'
CONVOCATORIA_MODIFICADA = 'convocatoria_modificada'
CALIFICACION_PUBLICADA = 'calificacion_publicada'
RETROALIMENTACION_PUBLICADA = 'retroalimentacion_publicada'
