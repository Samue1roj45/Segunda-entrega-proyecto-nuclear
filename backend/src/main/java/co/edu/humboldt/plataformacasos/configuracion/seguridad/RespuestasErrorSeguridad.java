package co.edu.humboldt.plataformacasos.configuracion.seguridad;

import co.edu.humboldt.plataformacasos.compartido.web.ErrorApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respuestas JSON para los rechazos de seguridad que ocurren antes de llegar a un controlador:
 * 401 cuando no hay sesión válida y 403 cuando la hay pero no alcanza.
 */
@Component
public class RespuestasErrorSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MENSAJE_NO_AUTENTICADO = "Debe iniciar sesión para acceder a este recurso.";
    static final String MENSAJE_ACCESO_DENEGADO = "No tiene permiso para realizar esta operación.";

    private final ObjectMapper objectMapper;

    public RespuestasErrorSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {
        escribir(respuesta, HttpStatus.UNAUTHORIZED, MENSAJE_NO_AUTENTICADO, solicitud.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest solicitud, HttpServletResponse respuesta,
                       AccessDeniedException excepcion) throws IOException {
        escribir(respuesta, HttpStatus.FORBIDDEN, MENSAJE_ACCESO_DENEGADO, solicitud.getRequestURI());
    }

    private void escribir(HttpServletResponse respuesta, HttpStatus estado, String mensaje, String ruta)
            throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(respuesta.getOutputStream(),
                ErrorApi.de(estado.value(), estado.getReasonPhrase(), mensaje, ruta));
    }
}
