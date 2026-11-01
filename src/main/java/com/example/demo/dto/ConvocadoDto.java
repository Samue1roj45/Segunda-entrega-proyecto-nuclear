package com.example.demo.dto;
/*
 #REQUISITO RF-04
 Para qué sirve: un estudiante autorizado en una sesión. NO expone el código de
 autorización (ese viaja solo en la notificación al estudiante, RF-20).
 */
public record ConvocadoDto(Long idAutorizacion, Long idEstudiante,
                           String nombre, String correo, String estado) {
}
