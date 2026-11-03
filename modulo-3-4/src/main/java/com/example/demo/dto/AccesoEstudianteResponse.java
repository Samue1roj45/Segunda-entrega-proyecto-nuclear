package com.example.demo.dto;

/*
 #REQUISITO RF-20
 Para qué sirve: respuesta al acceso válido. Con idAutorizacion el estudiante inicia
 su participación (POST /api/autorizaciones/{idAutorizacion}/participaciones).
 */
public record AccesoEstudianteResponse(Long idAutorizacion, Long idPractica, Long idUsuario,
                                       String nombre, String nombreCaso, String estadoSesion) {
}
