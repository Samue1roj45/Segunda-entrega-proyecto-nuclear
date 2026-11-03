package com.example.demo.event;

import com.example.demo.dto.ProgresoDto;
/*
 #REQUISITO RF-11
 Patrón: OBSERVER (evento)
 Para qué sirve: aviso de que un estudiante guardó/cambió una respuesta, con su
 progreso ya calculado. Cualquier observador interesado lo escucha.
 */
public record RespuestaRegistradaEvent(Long idParticipacion, Long idPregunta,
                                       Long idRespuesta, ProgresoDto progreso) {
}
