package com.example.demo.politica;

import com.example.demo.model.DatosTiempoSesion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
/*
 #REQUISITO RF-22
 Patrón: STRATEGY (estrategia concreta alternativa)
 Regla: usa el porcentaje guardado en practicas.porcentaje_limite_ingreso_tardio (50 si
 está vacío). ATENCIÓN: la BD (fn_ingreso_tardio) hoy ignora esa columna y usa siempre la
 mitad. Si se activa esta estrategia, hay que aplicar también el parche SQL opcional de
 la entrega, o la BD seguirá bloqueando a la mitad.
 */
public class PoliticaPorcentajeConfigurable implements PoliticaIngresoTardio {
    @Override
    public LocalDateTime limiteIngreso(DatosTiempoSesion d) {
        BigDecimal pct = d.porcentajeLimite() != null ? d.porcentajeLimite() : new BigDecimal("50");
        long minutos = BigDecimal.valueOf(d.tiempoMaximoMinutos())
                .multiply(pct)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                .longValue();
        return d.fechaInicio().plusMinutes(minutos);
    }
}
