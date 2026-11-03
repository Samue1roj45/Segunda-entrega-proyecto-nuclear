package com.example.demo.service.validacion;

import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.SesionProgramada;
import java.util.HashSet;
/*
 #REQUISITO RF-04
 Para qué sirve: debe haber uno o varios estudiantes y sin repetidos.
 (Que cada id sea realmente un ESTUDIANTE activo lo valida el trigger de la BD.)
 */
public class ValidadorEstudiantes extends ValidadorAgenda {
    @Override
    protected void comprobar(SesionProgramada s) {
        if (s.idsEstudiantes().isEmpty()) {
            throw new ReglaNegocioException("Debe asociar al menos un estudiante a la sesión");
        }
        if (new HashSet<>(s.idsEstudiantes()).size() != s.idsEstudiantes().size()) {
            throw new ReglaNegocioException("Hay estudiantes repetidos en la lista");
        }
    }
}
