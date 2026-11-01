package com.example.demo.service.validacion;

import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.SesionProgramada;
/*
 #REQUISITO RF-04
 Para qué sirve: al agendar, el docente debe asociar el nombre del espacio académico.
 */
public class ValidadorEspacioAcademico extends ValidadorAgenda {
    @Override
    protected void comprobar(SesionProgramada s) {
        if (s.espacioAcademico() == null || s.espacioAcademico().isBlank()) {
            throw new ReglaNegocioException("Debe indicar el espacio académico de la sesión");
        }
    }
}
