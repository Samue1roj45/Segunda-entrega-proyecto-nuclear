package com.example.demo.dto;

import jakarta.validation.constraints.Size;

/*
 #REQUISITO RF-25
 Para qué sirve: motivo (opcional) por el que el docente concede un intento extra.
*/
public record AutorizarRepeticionRequest(@Size(max = 255) String motivo) {
}
