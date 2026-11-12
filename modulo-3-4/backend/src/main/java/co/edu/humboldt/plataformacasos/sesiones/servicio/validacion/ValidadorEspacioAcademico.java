package co.edu.humboldt.plataformacasos.sesiones.servicio.validacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
// Patrón Chain of Responsibility: eslabón de agenda que valida el espacio académico (RF-04).
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
