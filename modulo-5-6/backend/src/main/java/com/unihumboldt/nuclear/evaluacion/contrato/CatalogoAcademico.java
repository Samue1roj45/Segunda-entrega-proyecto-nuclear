package com.unihumboldt.nuclear.evaluacion.contrato;

import java.util.List;
import java.util.Optional;

/**
 * Puerto hacia prácticas, autorizaciones, estudiantes y contenido del caso (módulos ajenos).
 * Esta implementación es temporal y se reemplazará por los servicios oficiales.
 */
public interface CatalogoAcademico {

    Optional<PracticaRef> buscarPractica(Long id);

    Optional<AutorizacionRef> buscarAutorizacion(Long id);

    Optional<EstudianteRef> buscarEstudiante(Long id);

    void actualizarAutorizacion(AutorizacionRef autorizacion);

    List<AutorizacionRef> autorizacionesDePractica(Long practicaId);

    List<PreguntaRef> preguntasDelCaso(Long casoId);

    Optional<RespuestaOpcionRef> buscarRespuesta(Long id);

    Optional<RubricaRef> buscarRubricaPorCaso(Long casoId);
}
