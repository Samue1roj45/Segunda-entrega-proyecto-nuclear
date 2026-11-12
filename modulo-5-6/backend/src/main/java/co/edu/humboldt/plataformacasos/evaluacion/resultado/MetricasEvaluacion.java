package com.unihumboldt.nuclear.evaluacion.resultado;

import java.math.BigDecimal;

public record MetricasEvaluacion(
        int correctas,
        int incorrectas,
        int noRespondidas,
        int pesoObtenido,
        int pesoTotal,
        int escalaMaxima,
        BigDecimal notaFinal) {
}
