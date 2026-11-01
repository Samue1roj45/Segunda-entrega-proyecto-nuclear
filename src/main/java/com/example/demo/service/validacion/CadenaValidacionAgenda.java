package com.example.demo.service.validacion;

import com.example.demo.model.SesionProgramada;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-04 || RF-18
 Patrón: CHAIN OF RESPONSIBILITY (armado de la cadena)
 Para qué sirve: único lugar donde se define el ORDEN de las validaciones.
 */
@Component
public class CadenaValidacionAgenda {

    private final ValidadorAgenda cabeza;

    public CadenaValidacionAgenda() {
        ValidadorAgenda espacio = new ValidadorEspacioAcademico();
        espacio.siguiente(new ValidadorFechas())
               .siguiente(new ValidadorEstudiantes());
        this.cabeza = espacio;
    }

    public void validar(SesionProgramada sesion) {
        cabeza.validar(sesion);
    }
}
