package co.edu.humboldt.plataformacasos.sesiones.servicio.validacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import java.time.LocalDateTime;
// Patrón Chain of Responsibility: eslabón de agenda que valida fechas de inicio y fin.
/*
 #REQUISITO RF-18
 Para qué sirve: fecha y horario de inicio/fin coherentes. El tope del tiempo máximo
 (no mayor al estimado del caso) lo valida ServicioSesiones con el módulo 2.
 */
public class ValidadorFechas extends ValidadorAgenda {
    @Override
    protected void comprobar(SesionProgramada s) {
        if (!s.fechaFin().isAfter(s.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior a la de inicio");
        }
        if (s.fechaFin().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de fin ya pasó");
        }
    }
}
