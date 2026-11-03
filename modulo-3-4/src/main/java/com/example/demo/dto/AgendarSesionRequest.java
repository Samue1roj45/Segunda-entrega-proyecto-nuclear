package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
/*
 #REQUISITO RF-04 || RF-18
 Para qué sirve: cuerpo JSON con el que el docente agenda un caso. Valida formato
 básico (nulos, vacíos, tamaños). Las reglas de fondo las aplican la cadena de
 validación (Java) y los triggers de la BD.
 */
public record AgendarSesionRequest(
        @NotNull Long idCaso,
        @NotBlank @Size(max = 150) String espacioAcademico,        // RF-04
        @NotNull LocalDateTime fechaInicio,                        // RF-18
        @NotNull LocalDateTime fechaFin,                           // RF-18
        @NotNull @Positive Integer tiempoMaximoMinutos,            // RF-18
        @Size(max = 150) String lugar,                             // RF-18: solo si aplica
        String mensajePersonalizado,                               // RF-18
        @Size(max = 255) String urlAcceso,
        @NotEmpty List<@NotNull Long> idsEstudiantes) {            // RF-04: uno o varios
}
