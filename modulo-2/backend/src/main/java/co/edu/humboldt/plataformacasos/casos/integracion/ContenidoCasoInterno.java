package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.casos.repositorio.CasoProgramable;

import java.util.Optional;

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

    /**
     * Datos generales de un caso publicado o archivado (nombre, tiempo estimado y docente creador), sin
     * exigir permiso: los usan los módulos 3 y 4 para mostrar la sesión y validar al docente creador (RF-25).
     */
    CasoProgramable resumen(Long idCaso);

    /** Caso al que pertenece una opción de respuesta; vacío si la opción no existe (RF-10, RF-13). */
    Optional<Long> casoDeRespuesta(Long idRespuesta);
}
