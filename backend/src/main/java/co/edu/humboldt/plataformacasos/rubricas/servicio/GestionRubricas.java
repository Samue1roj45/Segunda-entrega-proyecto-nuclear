package co.edu.humboldt.plataformacasos.rubricas.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CriterioDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosCriterioRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosRubricaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RubricaDTO;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

/** RF-17: rúbrica del caso y sus criterios de evaluación. */
public interface GestionRubricas {

    /** Crea la rúbrica del caso, que queda asociada a él, o actualiza la que ya tiene. */
    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    RubricaDTO registrar(Long idCaso, DatosRubricaRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    RubricaDTO consultar(Long idCaso);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    void retirar(Long idCaso);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    CriterioDTO agregarCriterio(Long idCaso, DatosCriterioRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    CriterioDTO actualizarCriterio(Long idCaso, Long idCriterio, DatosCriterioRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    void eliminarCriterio(Long idCaso, Long idCriterio);
}
