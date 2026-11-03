package com.example.demo.dto;
/*
 #REQUISITO RF-25
 Para qué sirve: intentos que el estudiante ha usado y cuántos tiene permitidos
 (1 + repeticiones autorizadas por el docente creador del caso).
 */
public record IntentosDto(Long idAutorizacion, int intentosUsados, int intentosPermitidos) {
}
