package co.edu.humboldt.plataformacasos.sesiones.politica;

import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosTiempoSesion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
/*
 #REQUISITO RF-22
 Patrón: STRATEGY (estrategia concreta alternativa)
 Regla: usa el porcentaje guardado en practicas.porcentaje_limite_ingreso_tardio (50 si
 está vacío). Se activa con app.sesiones.politica-ingreso-tardio=porcentaje.
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
