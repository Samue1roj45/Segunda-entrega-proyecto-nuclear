package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Pixel Streaming: el juego corre en un servidor y se ve dentro de la página del estudiante (sin salir de SOMA).
 * Aquí solo se publica el enlace del reproductor y el avance de la sesión que el juego ya informa por
 * {@code POST /sesiones/{id}/eventos}; el contrato ecos-soma/1 (canje, eventos, resultado) no cambia.
 */
@RestController
@RequestMapping("/api/ecos/v1")
public class PixelStreamingControlador {

    /** activo=false: no hay reproductor configurado y la página sigue con el enlace ecos://. */
    public record ConfiguracionStreaming(boolean activo, String url, String origen) {
    }

    /** Lo último que el juego informó de esta sesión (estado, punto y acto), para mostrarlo junto al reproductor. */
    public record AvanceSesion(String sesionId, String estado, String ultimoPunto, Integer ultimoActo, LocalDateTime ultimoEventoEn) {
    }

    private final ConfiguracionStreaming configuracion;
    private final ServicioAutorizacion autorizacion;
    private final JdbcTemplate jdbc;

    public PixelStreamingControlador(PropiedadesPixelStreaming propiedades, ServicioAutorizacion autorizacion, JdbcTemplate jdbc) {
        this.configuracion = configuracionDe(propiedades.url());
        this.autorizacion = autorizacion;
        this.jdbc = jdbc;
    }

    @GetMapping("/pixel-streaming")
    public ConfiguracionStreaming configuracion() {
        autorizacion.exigirPermiso(Permiso.PARTICIPAR_EN_CASOS);
        return configuracion;
    }

    @GetMapping("/sesiones/{sesionId}/avance")
    public AvanceSesion avance(@PathVariable String sesionId) {
        String estudiante = String.valueOf(autorizacion.exigirPermiso(Permiso.PARTICIPAR_EN_CASOS).id());
        List<AvanceSesion> filas = jdbc.query("""
                SELECT sesion_id, estado, ultimo_punto, ultimo_acto, ultimo_evento_en
                  FROM ecos_sesion WHERE sesion_id = ? AND estudiante_id = ?
                """, (rs, n) -> {
            Timestamp t = rs.getTimestamp(5);
            Object acto = rs.getObject(4);
            return new AvanceSesion(rs.getString(1), rs.getString(2), rs.getString(3),
                    acto == null ? null : ((Number) acto).intValue(), t == null ? null : t.toLocalDateTime());
        }, sesionId, estudiante);
        if (filas.isEmpty()) {
            // Misma respuesta si no existe o si es de otro estudiante: no se revela qué sesiones existen.
            throw new EcosException(HttpStatus.NOT_FOUND, "SESION_NO_EXISTE", "No encontramos esa sesión de juego.");
        }
        return filas.get(0);
    }

    /** Solo se acepta una URL http(s) absoluta; cualquier otra cosa deja el streaming apagado. */
    static ConfiguracionStreaming configuracionDe(String url) {
        String limpia = url == null ? "" : url.trim();
        if (limpia.isEmpty()) {
            return new ConfiguracionStreaming(false, "", "");
        }
        try {
            URI uri = new URI(limpia);
            String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!("http".equals(esquema) || "https".equals(esquema)) || uri.getHost() == null || uri.getUserInfo() != null) {
                return new ConfiguracionStreaming(false, "", "");
            }
            String origen = esquema + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
            return new ConfiguracionStreaming(true, limpia, origen);
        } catch (java.net.URISyntaxException e) {
            return new ConfiguracionStreaming(false, "", "");
        }
    }
}
