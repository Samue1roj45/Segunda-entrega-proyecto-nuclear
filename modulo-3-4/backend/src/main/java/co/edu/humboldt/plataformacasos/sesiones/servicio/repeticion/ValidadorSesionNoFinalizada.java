package com.example.demo.service.repeticion;

import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.SolicitudRepeticion;
/*
 #REQUISITO RF-25
 Para qué sirve: no se autoriza una repetición en una sesión ya finalizada.
 */
public class ValidadorSesionNoFinalizada extends ValidadorRepeticion {
    @Override
    protected void comprobar(SolicitudRepeticion s) {
        if ("FINALIZADO".equals(s.datos().estadoSesion())) {
            throw new ReglaNegocioException("No se puede autorizar una repetición en una sesión finalizada");
        }
    }
}
