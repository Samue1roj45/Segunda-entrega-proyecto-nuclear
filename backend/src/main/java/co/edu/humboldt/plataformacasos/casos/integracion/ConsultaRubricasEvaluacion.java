package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RubricaDTO;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

/** Contrato para el Módulo 4 (RF-13, RF-15): rúbrica con la que se evalúa un caso. */
public interface ConsultaRubricasEvaluacion {

    /** Disponible para casos publicados o archivados: los resultados históricos la siguen necesitando. */
    @RequierePermiso(Permiso.CONSULTAR_PARTICIPACIONES)
    RubricaDTO rubricaDe(Long idCaso);
}
