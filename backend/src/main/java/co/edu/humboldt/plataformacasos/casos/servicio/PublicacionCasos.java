package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.validacion.InformeValidacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

/** RF-09: validación académica del caso. */
public interface PublicacionCasos {

    /** Evalúa las reglas sin cambiar el estado del caso. */
    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    InformeValidacion validar(Long idCaso);
}
