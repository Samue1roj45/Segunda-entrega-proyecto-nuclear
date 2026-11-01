package com.example.demo.exception;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
/*
 #REQUISITO Transversal (todos los RF)
 Para qué sirve: la BD rechaza operaciones con SIGNAL SQLSTATE '45000' y un mensaje
 legible (triggers y procedimientos). Aquí ese mensaje se convierte en HTTP 409 para
 que el frontend lo muestre tal cual. También unifica los demás errores en JSON.
 (Versión tanda 2: agrega 401 para RF-20.)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, String>> reglaNegocio(ReglaNegocioException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<Map<String, String>> noAutenticado(NoAutenticadoException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, String>> accesoDenegado(AccesoDenegadoException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, String>> cabeceraFaltante(MissingRequestHeaderException ex) {
        return error(HttpStatus.BAD_REQUEST, "Falta la cabecera " + ex.getHeaderName());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException ex) {
        SQLException sql = SqlErrores.buscar(ex);
        if (sql != null && "45000".equals(sql.getSQLState())) {
            return error(HttpStatus.CONFLICT, sql.getMessage());
        }
        return error(HttpStatus.CONFLICT,
                "Dato inválido: algún identificador no existe o la opción no pertenece a la pregunta");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> baseDeDatos(DataAccessException ex) {
        SQLException sql = SqlErrores.buscar(ex);
        if (sql != null && "45000".equals(sql.getSQLState())) {
            return error(HttpStatus.CONFLICT, sql.getMessage());   // regla de la BD
        }
        log.error("Error de base de datos", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno de base de datos");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
