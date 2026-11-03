package com.example.demo.exception;
/*
 #REQUISITO RF-20
 Para qué sirve: correo o código de autorización inválidos, vencidos o sesión finalizada.
 */
public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
