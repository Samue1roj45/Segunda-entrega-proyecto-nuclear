package com.example.demo.service.repeticion;

import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.model.SolicitudRepeticion;
/*
 #REQUISITO RF-25
 Para qué sirve: solo el docente CREADOR del caso puede autorizar una repetición.
 */
public class ValidadorDocenteCreador extends ValidadorRepeticion {
    @Override
    protected void comprobar(SolicitudRepeticion s) {
        if (!s.idDocente().equals(s.datos().idDocenteCreador())) {
            throw new AccesoDenegadoException("Solo el docente creador del caso puede autorizar una repetición");
        }
    }
}
