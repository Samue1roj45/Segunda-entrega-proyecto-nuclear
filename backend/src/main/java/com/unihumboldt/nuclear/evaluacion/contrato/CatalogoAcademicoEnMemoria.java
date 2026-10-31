package com.unihumboldt.nuclear.evaluacion.contrato;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Catálogo temporal en memoria. No es el módulo de prácticas ni el académico.
 */
@Component
public class CatalogoAcademicoEnMemoria implements CatalogoAcademico {

    private final Map<Long, PracticaRef> practicas = new ConcurrentHashMap<>();
    private final Map<Long, AutorizacionRef> autorizaciones = new ConcurrentHashMap<>();
    private final Map<Long, EstudianteRef> estudiantes = new ConcurrentHashMap<>();
    private final Map<Long, PreguntaRef> preguntas = new ConcurrentHashMap<>();
    private final Map<Long, RespuestaOpcionRef> respuestas = new ConcurrentHashMap<>();
    private final Map<Long, RubricaRef> rubricasPorCaso = new ConcurrentHashMap<>();

    public void limpiar() {
        practicas.clear();
        autorizaciones.clear();
        estudiantes.clear();
        preguntas.clear();
        respuestas.clear();
        rubricasPorCaso.clear();
    }

    public void registrar(PracticaRef practica) {
        practicas.put(practica.id(), practica);
    }

    public void registrar(AutorizacionRef autorizacion) {
        autorizaciones.put(autorizacion.id(), autorizacion);
    }

    public void registrar(EstudianteRef estudiante) {
        estudiantes.put(estudiante.id(), estudiante);
    }

    public void registrar(PreguntaRef pregunta) {
        preguntas.put(pregunta.id(), pregunta);
    }

    public void registrar(RespuestaOpcionRef respuesta) {
        respuestas.put(respuesta.id(), respuesta);
    }

    public void registrar(RubricaRef rubrica) {
        rubricasPorCaso.put(rubrica.casoId(), rubrica);
    }

    @Override
    public Optional<PracticaRef> buscarPractica(Long id) {
        return Optional.ofNullable(practicas.get(id));
    }

    @Override
    public Optional<AutorizacionRef> buscarAutorizacion(Long id) {
        return Optional.ofNullable(autorizaciones.get(id));
    }

    @Override
    public Optional<EstudianteRef> buscarEstudiante(Long id) {
        return Optional.ofNullable(estudiantes.get(id));
    }

    @Override
    public void actualizarAutorizacion(AutorizacionRef autorizacion) {
        autorizaciones.put(autorizacion.id(), autorizacion);
    }

    @Override
    public List<AutorizacionRef> autorizacionesDePractica(Long practicaId) {
        return autorizaciones.values().stream()
                .filter(a -> a.practicaId().equals(practicaId))
                .toList();
    }

    @Override
    public List<PreguntaRef> preguntasDelCaso(Long casoId) {
        List<PreguntaRef> out = new ArrayList<>();
        for (PreguntaRef pregunta : preguntas.values()) {
            if (pregunta.casoId().equals(casoId)) {
                out.add(pregunta);
            }
        }
        return out;
    }

    @Override
    public Optional<RespuestaOpcionRef> buscarRespuesta(Long id) {
        return Optional.ofNullable(respuestas.get(id));
    }

    @Override
    public Optional<RubricaRef> buscarRubricaPorCaso(Long casoId) {
        return Optional.ofNullable(rubricasPorCaso.get(casoId));
    }
}
