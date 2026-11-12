package co.edu.humboldt.plataformacasos.sesiones.evento;

import co.edu.humboldt.plataformacasos.sesiones.dto.ProgresoDto;

/*
 #REQUISITO RF-11
 Patrón: OBSERVER (evento)
 Para qué sirve: aviso de que un estudiante guardó/cambió una respuesta, con su
 progreso ya calculado. Cualquier observador interesado lo escucha.
 */
public record RespuestaRegistradaEvento(Long idParticipacion, Long idPregunta,
                                        Long idRespuesta, ProgresoDto progreso) {
}
