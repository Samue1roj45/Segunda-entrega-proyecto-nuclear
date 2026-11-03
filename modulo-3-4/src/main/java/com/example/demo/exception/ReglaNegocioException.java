package com.example.demo.exception;
/*
 #REQUISITO Transversal
 Para qué sirve: error de regla de negocio detectado en Java (antes de llegar a la BD).
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
