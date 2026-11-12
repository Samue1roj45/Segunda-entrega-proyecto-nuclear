package co.edu.humboldt.plataformacasos.soporte;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Solo en pruebas: imprime cada petición HTTP que hacen las pruebas de API y lo que respondió el
 * backend (método, ruta, cuerpo enviado, estado y cuerpo recibido). Las claves se ocultan y los
 * tokens se recortan. Se carga con META-INF/spring/...AutoConfiguration.imports de src/test.
 */
@AutoConfiguration
public class ReporteHttpPruebas {

    private static final int MAX = 400;

    @Bean
    MockMvcBuilderCustomizer reporteHttpEnConsola() {
        return builder -> builder.alwaysDo(resultado -> {
            MockHttpServletRequest req = resultado.getRequest();
            MockHttpServletResponse res = resultado.getResponse();
            String ruta = req.getRequestURI() + (req.getQueryString() == null ? "" : "?" + req.getQueryString());
            String cuerpoReq = req.getContentAsString();
            System.out.println("    -> " + req.getMethod() + " " + ruta + (vacio(cuerpoReq) ? "" : "  " + limpiar(cuerpoReq)));
            HttpStatus estado = HttpStatus.resolve(res.getStatus());
            String cuerpoRes = res.getContentType() != null && res.getContentType().contains("json")
                    ? limpiar(res.getContentAsString(StandardCharsets.UTF_8))
                    : (res.getContentAsByteArray().length > 0 ? "[" + res.getContentType() + ", " + res.getContentAsByteArray().length + " bytes]" : "");
            System.out.println("    <- " + res.getStatus() + (estado == null ? "" : " " + estado.getReasonPhrase())
                    + (vacio(cuerpoRes) ? "" : "  " + cuerpoRes));
        });
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static String limpiar(String json) {
        String s = json.replaceAll("\\s*\\n\\s*", " ")
                .replaceAll("(\"(?:clave|password|claveActual|claveNueva|codigo|codigoAcceso)\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
                .replaceAll("(\"token\"\\s*:\\s*\")([^\"]{12})[^\"]*\"", "$1$2...\"");
        return s.length() > MAX ? s.substring(0, MAX) + " ..." : s;
    }
}
