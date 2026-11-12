package co.edu.humboldt.plataformacasos.sesiones.servicio.validacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import java.time.Duration;

// Patrón Chain of Responsibility: eslabón de agenda que valida el tiempo máximo por intento.
/*
 #REQUISITO RF-18 || RF-23
 Para qué sirve: el tiempo máximo de un intento debe caber dentro del horario de la sesión; si no,
 ningún estudiante podría usarlo completo. (Que no supere el tiempo estimado del caso se valida en
 ServicioSesiones con los datos del módulo 2.)
 */
public class ValidadorTiempoMaximo extends ValidadorAgenda {
    @Override
    protected void comprobar(SesionProgramada s) {
        long minutosSesion = Duration.between(s.fechaInicio(), s.fechaFin()).toMinutes();
        if (s.tiempoMaximoMinutos() <= 0) {
            throw new ReglaNegocioException("El tiempo máximo debe ser mayor que cero");
        }
        if (s.tiempoMaximoMinutos() > minutosSesion) {
            throw new ReglaNegocioException("El tiempo máximo (" + s.tiempoMaximoMinutos()
                    + " min) no cabe en el horario de la sesión (" + minutosSesion + " min)");
        }
    }
}
