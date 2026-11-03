package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
/*
 #REQUISITO RF-10 || RF-11
 Para qué sirve: la opción que el estudiante elige (o cambia) para una pregunta.
 */
public record ResponderRequest(@NotNull Long idRespuesta) {
}
