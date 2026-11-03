package co.edu.humboldt.plataformacasos.compartido.web;

import co.edu.humboldt.plataformacasos.autenticacion.servicio.CredencialesInvalidasException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.NoAutenticadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorApi> parametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.BAD_REQUEST, "Valor no válido para '" + e.getName() + "'.", solicitud);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    ResponseEntity<ErrorApi> noAutenticado(NoAutenticadoException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage(), solicitud);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<ErrorApi> accesoDenegado(AccesoDenegadoException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.FORBIDDEN, e.getMessage(), solicitud);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ErrorApi> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), solicitud);
    }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<ErrorApi> conflicto(ConflictoException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), solicitud);
    }

    /** Respaldo ante carreras: la restricción UNIQUE de la base también se traduce a 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> integridad(DataIntegrityViolationException e, HttpServletRequest solicitud) {
        return responder(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes.", solicitud);
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
