package com.example.demo.model;
/*
 #REQUISITO RF-25
 Para qué sirve: lo que se necesita saber de una autorización para decidir si se puede
 conceder un intento extra: estado, quién creó el caso, quién programó la sesión y cuántos
 intentos lleva (los AUSENTE no cuentan: no participó).
 */
public record DatosRepeticion(String estadoAutorizacion, Long idDocenteCreador, Long idDocenteSesion,
                              String estadoSesion, int intentosUsados, int repeticionesAutorizadas) {

    /** RF-25: 1 intento + 1 por cada repetición autorizada. */
    public int intentosPermitidos() {
        return 1 + repeticionesAutorizadas;
    }
}
