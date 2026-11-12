package com.example.demo.service.repeticion;

import com.example.demo.model.SolicitudRepeticion;
/*
 #REQUISITO RF-25
 Patrón: CHAIN OF RESPONSIBILITY (eslabón base)
 Para qué sirve: cada validador comprueba UNA condición para conceder un intento extra
 y pasa al siguiente; si falla, lanza excepción y la cadena se corta.
 */
public abstract class ValidadorRepeticion {

    private ValidadorRepeticion siguiente;

    public ValidadorRepeticion siguiente(ValidadorRepeticion siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(SolicitudRepeticion solicitud) {
        comprobar(solicitud);
        if (siguiente != null) {
            siguiente.validar(solicitud);
        }
    }

    protected abstract void comprobar(SolicitudRepeticion solicitud);
}
