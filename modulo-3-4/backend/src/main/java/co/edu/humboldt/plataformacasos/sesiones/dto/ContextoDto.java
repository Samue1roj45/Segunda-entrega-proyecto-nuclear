package com.example.demo.dto;

import java.util.List;

/*
 #REQUISITO RF-10
 Para qué sirve: lo que el estudiante ve durante su participación: contexto del caso,
 escenarios, preguntas y opciones, más la opción que tiene marcada en cada pregunta
 (para poder volver y cambiarla).
 SEGURIDAD: a propósito NO incluye es_correcta ni retroalimentación de las opciones.
 */
public record ContextoDto(String nombreCaso, String contextoHistoria,
                          String desarrolloSituacional, List<Escenario> escenarios) {

    public record Escenario(Long idEscenario, int orden, String nombre,
                            String contexto, List<Pregunta> preguntas) { }

    public record Pregunta(Long idPregunta, int orden, String enunciado,
                           Long idRespuestaSeleccionada, List<Opcion> opciones) { }

    public record Opcion(Long idRespuesta, int orden, String texto) { }
}
