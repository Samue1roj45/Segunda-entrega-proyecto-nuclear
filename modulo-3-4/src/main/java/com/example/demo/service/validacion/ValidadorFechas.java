package com.example.demo.service.validacion;

import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.SesionProgramada;
import java.time.LocalDateTime;
/*
 #REQUISITO RF-18
 Para qué sirve: fecha y horario de inicio/fin coherentes. El tope del tiempo máximo
 (no mayor al estimado del caso) lo valida el trigger de la BD.
 */
public class ValidadorFechas extends ValidadorAgenda {
    @Override
    protected void comprobar(SesionProgramada s) {
        if (!s.fechaFin().isAfter(s.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin debe ser posterior a la de inicio");
        }
        if (s.fechaFin().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de fin ya pasó");
        }
    }
}
