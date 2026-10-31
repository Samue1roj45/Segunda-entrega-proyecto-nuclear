package com.unihumboldt.nuclear.evaluacion.resultado;

public record DetallePreguntaResultado(
        Long preguntaId,
        String enunciado,
        int peso,
        boolean respondida,
        Long respuestaElegidaId,
        boolean correcta) {
}
