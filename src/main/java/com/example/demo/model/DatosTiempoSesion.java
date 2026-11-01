package com.example.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/*
 #REQUISITO RF-22
 Para qué sirve: datos de tiempo de la sesión de una autorización, tal como los ve la BD
 (incluida la hora actual de la BD en `ahora`, para no depender del reloj de Java).
 */
public record DatosTiempoSesion(LocalDateTime fechaInicio, int tiempoMaximoMinutos,
                                BigDecimal porcentajeLimite, String estadoSesion,
                                String estadoAutorizacion, LocalDateTime ahora) {
}
