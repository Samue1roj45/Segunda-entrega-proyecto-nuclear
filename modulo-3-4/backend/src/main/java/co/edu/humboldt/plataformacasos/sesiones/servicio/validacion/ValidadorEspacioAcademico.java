package co.edu.humboldt.plataformacasos.sesiones.servicio.validacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
/*
 #REQUISITO RF-04
 Para qué sirve: al agendar, el docente debe asociar el nombre del espacio académico.
 */
public class ValidadorEspacioAcademico extends ValidadorAgenda {
    @Override
    protected void comprobar(SesionProgramada s) {
        if (s.espacioAcademico() == null || s.espacioAcademico().isBlank()) {
            throw new ReglaNegocioException("Debe indicar el espacio académico de la sesión");
        }
    }
}
