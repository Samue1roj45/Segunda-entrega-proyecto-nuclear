package co.edu.humboldt.plataformacasos.ecos;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/** Responde los errores del contrato con el código estable (SESION_YA_USADA, CASO_DISTINTO...). */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ManejadorErroresEcos {

    record ErrorEcos(LocalDateTime fecha, int estado, String codigo, String error, String mensaje, String ruta) {
    }

    @ExceptionHandler(EcosException.class)
    ResponseEntity<ErrorEcos> alFallar(EcosException e, HttpServletRequest solicitud) {
        var cuerpo = new ErrorEcos(LocalDateTime.now(), e.estado().value(), e.codigo(),
                e.estado().getReasonPhrase(), e.getMessage(), solicitud.getRequestURI());
        return ResponseEntity.status(e.estado()).body(cuerpo);
    }
}
