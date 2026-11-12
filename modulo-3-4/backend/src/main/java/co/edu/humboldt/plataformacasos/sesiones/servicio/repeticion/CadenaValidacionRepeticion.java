package com.example.demo.service.repeticion;

import com.example.demo.model.SolicitudRepeticion;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-25
 Patrón: CHAIN OF RESPONSIBILITY (armado de la cadena)
 Para qué sirve: único lugar donde se define el orden de las validaciones.
 */
@Component
public class CadenaValidacionRepeticion {

    private final ValidadorRepeticion cabeza;

    public CadenaValidacionRepeticion() {
        ValidadorRepeticion activa = new ValidadorAutorizacionActiva();
        activa.siguiente(new ValidadorDocenteCreador())
              .siguiente(new ValidadorSesionNoFinalizada());
        this.cabeza = activa;
    }

    public void validar(SolicitudRepeticion solicitud) {
        cabeza.validar(solicitud);
    }
}
