package co.edu.humboldt.plataformacasos.compartido.web;

import co.edu.humboldt.plataformacasos.autenticacion.servicio.CredencialesInvalidasException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de la aplicación a respuestas HTTP con el cuerpo ErrorApi. */
@RestControllerAdvice
public class ManejadorErroresApi {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErroresApi.class);

    @ExceptionHandler(SolicitudInvalidaException.class)
    ResponseEntity<ErrorApi> solicitudInvalida(SolicitudInvalidaException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), solicitud);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorApi> cuerpoIlegible(HttpMessageNotReadableException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es un JSON válido.", solicitud);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<ErrorApi> credencialesInvalidas(CredencialesInvalidasException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage(), solicitud);
    }

    /**
     * Cualquier otro error. Las excepciones propias de Spring MVC (404, 405, 415...) conservan su
     * código; las no previstas se registran completas y al cliente no se le exponen detalles internos.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorApi> errorInesperado(Exception e, HttpServletRequest solicitud) {
        if (e instanceof ErrorResponse errorSpring) {
            HttpStatus estado = HttpStatus.valueOf(errorSpring.getStatusCode().value());
            return responder(estado, estado.getReasonPhrase(), solicitud);
        }
        LOG.error("Error no controlado en {}", solicitud.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado.", solicitud);
    }

    private static ResponseEntity<ErrorApi> responder(HttpStatus estado, String mensaje, HttpServletRequest solicitud) {
        return ResponseEntity.status(estado)
                .body(ErrorApi.de(estado.value(), estado.getReasonPhrase(), mensaje, solicitud.getRequestURI()));
    }
}
