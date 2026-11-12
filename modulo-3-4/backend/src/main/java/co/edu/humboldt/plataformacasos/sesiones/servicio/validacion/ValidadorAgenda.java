package com.example.demo.service.validacion;

import com.example.demo.model.SesionProgramada;
/*
 #REQUISITO RF-04 || RF-18
 Patrón: CHAIN OF RESPONSIBILITY (eslabón base)
 Para qué sirve: cada validador revisa UNA regla y pasa al siguiente. Si falla,
 lanza ReglaNegocioException y la cadena se corta. Agregar una regla nueva =
 crear una clase y enlazarla (principio Abierto/Cerrado, SOLID).
 */
public abstract class ValidadorAgenda {

    private ValidadorAgenda siguiente;

    /** Enlaza el siguiente eslabón y lo devuelve para encadenar llamadas. */
    public ValidadorAgenda siguiente(ValidadorAgenda siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(SesionProgramada sesion) {
        comprobar(sesion);
        if (siguiente != null) {
            siguiente.validar(sesion);
        }
    }

    protected abstract void comprobar(SesionProgramada sesion);
}
