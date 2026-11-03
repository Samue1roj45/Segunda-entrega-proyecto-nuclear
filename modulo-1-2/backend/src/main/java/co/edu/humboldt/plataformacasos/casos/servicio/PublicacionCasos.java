package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.validacion.InformeValidacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

/** RF-09: validación académica, publicación y archivo del caso. */
public interface PublicacionCasos {

    /** Evalúa las reglas sin cambiar el estado del caso. */
    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    InformeValidacion validar(Long idCaso);

    /** Publica el caso si cumple todas las reglas; si no, lanza el informe y sigue en BORRADOR. */
    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    CasoDetalleDTO publicar(Long idCaso);

    /** Retira el caso publicado de las nuevas programaciones sin borrar datos. */
    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    CasoDetalleDTO archivar(Long idCaso);
}
