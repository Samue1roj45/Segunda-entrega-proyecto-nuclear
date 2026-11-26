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

/**
 * Devuelve la versión del caso después de guardarlo, también cuando la respuesta
 * no es el detalle completo (escenario, pregunta, rúbrica).
 */
@RestControllerAdvice
class VersionCasoRespuesta implements org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(org.springframework.core.MethodParameter returnType,
                            Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, org.springframework.core.MethodParameter returnType,
                                  org.springframework.http.MediaType selectedContentType,
                                  Class<? extends org.springframework.http.converter.HttpMessageConverter<?>> selectedConverterType,
                                  org.springframework.http.server.ServerHttpRequest request,
                                  org.springframework.http.server.ServerHttpResponse response) {
        org.springframework.web.context.request.RequestAttributes atributos =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (atributos == null
                || !(response instanceof org.springframework.http.server.ServletServerHttpResponse servlet)) {
            return body;
        }
        Object caso = atributos.getAttribute(
                co.edu.humboldt.plataformacasos.casos.servicio.AccesoCasos.ATRIBUTO_CASO,
                org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
        if (caso instanceof co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio estudio) {
            servlet.getServletResponse().setHeader(
                    co.edu.humboldt.plataformacasos.casos.servicio.AccesoCasos.ENCABEZADO_VERSION,
                    Long.toString(estudio.getVersion()));
        }
        return body;
    }
}
