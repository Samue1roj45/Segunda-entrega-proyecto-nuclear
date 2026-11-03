package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosPreguntaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.PreguntaDTO;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

/** RF-06 y RF-07: preguntas de cada escenario. */
public interface GestionPreguntas {

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    PreguntaDTO agregar(Long idCaso, Long idEscenario, DatosPreguntaRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    PreguntaDTO consultar(Long idCaso, Long idPregunta);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    PreguntaDTO actualizar(Long idCaso, Long idPregunta, DatosPreguntaRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    void eliminar(Long idCaso, Long idPregunta);
}
