package co.edu.humboldt.plataformacasos.casos.integracion;

/**
 * Puerto interno para los Módulos 3 y 4, sin endpoint a propósito: quien lo llama debe haber comprobado
 * antes la convocatoria y el horario del estudiante (Módulo 3) o el fin del intento (Módulo 4). Exponerlo
 * por HTTP permitiría a cualquier estudiante leer un caso sin estar convocado.
 */
public interface ContenidoCasoInterno {

    /** Contenido sin clave para mostrar durante la participación. */
    ContenidoParticipacionDTO contenidoSinClaves(Long idCaso);

    /** Contenido con clave, porcentajes, justificaciones y rúbrica para evaluar. */
    ClaveEvaluacionDTO claveDeEvaluacion(Long idCaso);
}
