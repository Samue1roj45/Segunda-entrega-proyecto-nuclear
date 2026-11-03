package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/*
 #REQUISITO RF-20
 Para qué sirve: credenciales con las que el estudiante entra a una sesión:
 correo institucional + código de autorización que recibió por correo.
 */

public record AccesoEstudianteRequest(@NotBlank @Size(max = 120) String correo,
                                      @NotBlank @Size(max = 20) String codigo) {
}
