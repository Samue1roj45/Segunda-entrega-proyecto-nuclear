package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.validacion.CasoNoPublicableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Responde 422 con las reglas incumplidas. Va antes que ManejadorErroresApi del Módulo 1, cuyo
 * manejador general de Exception también atraparía esta excepción; los demás errores siguen allá.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejadorErroresPublicacion {

    @ExceptionHandler(CasoNoPublicableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorPublicacionApi casoNoPublicable(CasoNoPublicableException e, HttpServletRequest solicitud) {
        HttpStatus estado = HttpStatus.UNPROCESSABLE_ENTITY;
        return new ErrorPublicacionApi(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(),
                e.getMessage(), solicitud.getRequestURI(), e.informe().incumplimientos());
    }
}
