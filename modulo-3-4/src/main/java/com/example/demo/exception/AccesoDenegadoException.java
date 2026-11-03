package com.example.demo.exception;
/*
 #REQUISITO RF-10 || RF-11
 Para qué sirve: el estudiante intentó usar una participación que no es suya.
 */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
