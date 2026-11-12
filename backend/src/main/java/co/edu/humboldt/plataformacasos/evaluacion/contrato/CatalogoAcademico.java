package co.edu.humboldt.plataformacasos.evaluacion.contrato;

import java.util.List;
import java.util.Optional;

// Patrón Puerto (Ports & Adapters): M5 consulta prácticas y autorizaciones; el adaptador CatalogoAcademicoIntegrado lo implementa con M3.
/**
 * Puerto hacia los datos de otros módulos: sesiones y autorizaciones (módulo 3), estudiantes (módulo 1)
 * y preguntas, opciones y rúbrica del caso (módulo 2). Lo implementa la aplicación
 * (integracion.CatalogoAcademicoIntegrado); las pruebas del módulo usan un doble en memoria.
 */
public interface CatalogoAcademico {

    Optional<PracticaRef> buscarPractica(Long id);

    Optional<AutorizacionRef> buscarAutorizacion(Long id);

    Optional<EstudianteRef> buscarEstudiante(Long id);

    /** Solo las autorizaciones ACTIVAS (un estudiante retirado no se evalúa ni se notifica). */
    List<AutorizacionRef> autorizacionesDePractica(Long practicaId);

    List<PreguntaRef> preguntasDelCaso(Long casoId);

    Optional<RespuestaOpcionRef> buscarRespuesta(Long id);

    Optional<RubricaRef> buscarRubricaPorCaso(Long casoId);
}
