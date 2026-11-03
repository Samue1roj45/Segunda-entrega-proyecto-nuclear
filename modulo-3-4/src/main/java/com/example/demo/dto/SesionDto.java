package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
/*
 #REQUISITO RF-18 || RF-19
 Para qué sirve: vista de una sesión programada: condiciones (fechas, tiempo máximo,
 lugar, mensaje), estado actual y las acciones que el estado permite (INICIAR/FINALIZAR),
 para que el frontend sepa qué botones mostrar.
 */
public record SesionDto(Long idPractica, Long idCaso, String nombreCaso, String espacioAcademico,
                        LocalDateTime fechaInicio, LocalDateTime fechaFin, Integer tiempoMaximoMinutos,
                        String lugar, String mensajePersonalizado, String urlAcceso, String estado,
                        LocalDateTime fechaInicioReal, LocalDateTime fechaCierreReal, String tipoCierre,
                        List<String> accionesPermitidas) {

    public SesionDto conAcciones(List<String> acciones) {
        return new SesionDto(idPractica, idCaso, nombreCaso, espacioAcademico, fechaInicio, fechaFin,
                tiempoMaximoMinutos, lugar, mensajePersonalizado, urlAcceso, estado,
                fechaInicioReal, fechaCierreReal, tipoCierre, acciones);
    }
}
