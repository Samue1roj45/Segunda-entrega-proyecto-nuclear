package co.edu.humboldt.plataformacasos.ecos;

import org.springframework.http.HttpStatus;

/** Error del contrato ecos-soma/1: lleva el estado HTTP y el código estable que el juego sabe interpretar. */
public class EcosException extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;

    public EcosException(HttpStatus estado, String codigo, String mensaje) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus estado() {
        return estado;
    }

    public String codigo() {
        return codigo;
    }
}
