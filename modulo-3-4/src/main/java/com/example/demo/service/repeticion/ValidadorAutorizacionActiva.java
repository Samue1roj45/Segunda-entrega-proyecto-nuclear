package com.example.demo.service.repeticion;

import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.SolicitudRepeticion;
/*
 #REQUISITO RF-25
 Para qué sirve: no se concede un intento extra a un estudiante retirado de la sesión.
 */
public class ValidadorAutorizacionActiva extends ValidadorRepeticion {
    @Override
    protected void comprobar(SolicitudRepeticion s) {
        if (!"ACTIVA".equals(s.datos().estadoAutorizacion())) {
            throw new ReglaNegocioException("La autorización del estudiante fue retirada");
        }
    }
}
