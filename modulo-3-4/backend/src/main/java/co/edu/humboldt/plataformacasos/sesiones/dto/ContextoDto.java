package co.edu.humboldt.plataformacasos.sesiones.dto;

import java.util.List;

/*
 #REQUISITO RF-10
 Para qué sirve: lo que el estudiante ve durante su participación: contexto del caso,
 escenarios, preguntas y opciones, más la opción que tiene marcada en cada pregunta
 (para poder volver y cambiarla).
 SEGURIDAD: a propósito NO incluye es_correcta, porcentaje ni justificación de las opciones: se arma con
 el contrato ContenidoCasoInterno.contenidoSinClaves del módulo 2, que no tiene esos campos.
 */
public record ContextoDto(String nombreCaso, String contextoHistoria,
                          String desarrolloSituacional, List<Escenario> escenarios) {

    public record Escenario(Long idEscenario, int orden, String nombre,
                            String contexto, List<Pregunta> preguntas) { }

    public record Pregunta(Long idPregunta, int orden, String enunciado,
                           Long idRespuestaSeleccionada, List<Opcion> opciones) { }

    public record Opcion(Long idRespuesta, String codigo, String texto) { }
}
