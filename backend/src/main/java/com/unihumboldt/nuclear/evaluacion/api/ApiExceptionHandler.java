package com.unihumboldt.nuclear.evaluacion.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.unihumboldt.nuclear.evaluacion.participacion.AccesoDenegadoException;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;
import com.unihumboldt.nuclear.evaluacion.participacion.ReglaNegocioException;
import com.unihumboldt.nuclear.evaluacion.participacion.TransicionInvalidaException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({ReglaNegocioException.class, TransicionInvalidaException.class})
    public ResponseEntity<Map<String, String>> regla(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("detail", ex.getMessage()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, String>> denegado(AccesoDenegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("detail", ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("detail", "Solicitud inválida."));
    }
}
