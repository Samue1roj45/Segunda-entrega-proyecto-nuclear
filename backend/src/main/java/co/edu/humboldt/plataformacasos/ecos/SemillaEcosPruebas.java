package co.edu.humboldt.plataformacasos.ecos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Datos FICTICIOS del perfil «pruebas» (nunca en producción): deja a Carlos Gómez con el caso «Ecos de una decisión»
 * (creado desde el banco real) y una sesión SIN_INICIAR con Ana y Luis convocados. Los códigos de acceso son los de
 * siempre (CODIGO_VALIDO_01 y CODIGO_VALIDO_02, documento PT-DCP-01) y se guardan solo como hash BCrypt.
 * Es idempotente: si el caso o la sesión ya existen, no hace nada. Corre después de cargar el catálogo y de Flyway,
 * por eso el caso no puede sembrarse en una migración.
 */
@Component
@Profile("pruebas")
@Order(100)
class SemillaEcosPruebas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SemillaEcosPruebas.class);
    private static final String DOCENTE = "carlos.gomez@institucion.edu.co";
    private static final String CASO = "Ecos de una decisión";
    private static final Map<String, String> CODIGOS = new java.util.LinkedHashMap<>();

    static {
        CODIGOS.put("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01");
        CODIGOS.put("luis.rios@institucion.edu.co", "CODIGO_VALIDO_02");
    }

    private final JdbcTemplate jdbc;
    private final CasoDesdeEcos casoDesdeEcos;

    SemillaEcosPruebas(JdbcTemplate jdbc, CasoDesdeEcos casoDesdeEcos) {
        this.jdbc = jdbc;
        this.casoDesdeEcos = casoDesdeEcos;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Long> docente = jdbc.queryForList("SELECT id FROM usuario WHERE correo_institucional = ?", Long.class, DOCENTE);
            if (docente.isEmpty()) return;
            long docenteId = docente.get(0);
            if (jdbc.queryForObject("SELECT COUNT(*) FROM ecos_caso", Integer.class) == 0) return;
            if (jdbc.queryForObject("SELECT COUNT(*) FROM casos_estudio WHERE id_docente = ? AND nombre = ? AND fecha_eliminacion IS NULL",
                    Integer.class, docenteId, CASO) == 0) {
                actuarComo(docenteId);
                try {
                    casoDesdeEcos.crear(docenteId);
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }
            sembrarSesion(docenteId);
        } catch (Exception e) {
            log.warn("No se pudo sembrar el caso y la sesión de prueba de Ecos: {}", e.getMessage());
        }
    }

    private void sembrarSesion(long docenteId) {
        List<Map<String, Object>> caso = jdbc.queryForList(
                "SELECT id_caso, tiempo_estimado_min FROM casos_estudio WHERE id_docente = ? AND nombre = ? AND fecha_eliminacion IS NULL",
                docenteId, CASO);
        if (caso.isEmpty()) return;
        long idCaso = ((Number) caso.get(0).get("id_caso")).longValue();
        if (jdbc.queryForObject("SELECT COUNT(*) FROM practicas WHERE id_caso = ?", Integer.class, idCaso) > 0) return;
        int minutos = ((Number) caso.get(0).get("tiempo_estimado_min")).intValue();
        Timestamp inicio = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        Timestamp fin = Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusDays(30));
        jdbc.update("""
                INSERT INTO practicas (id_caso, id_docente, espacio_academico, fecha_inicio, fecha_fin,
                                       tiempo_maximo_minutos, lugar, mensaje_personalizado, estado_practica, fecha_creacion)
                VALUES (?, ?, 'Psicología', ?, ?, ?, 'Laboratorio 3', 'Sesión de prueba de Ecos de una decisión.', 'SIN_INICIAR', ?)
                """, idCaso, docenteId, inicio, fin, minutos, inicio);
        long idPractica = jdbc.queryForObject("SELECT MAX(id_practica) FROM practicas WHERE id_caso = ?", Long.class, idCaso);
        var codificador = new BCryptPasswordEncoder();
        for (Map.Entry<String, String> codigo : CODIGOS.entrySet()) {
            jdbc.update("""
                    INSERT INTO autorizaciones (id_practica, id_estudiante, codigo_hash, estado, fecha_creacion)
                    SELECT ?, id, ?, 'ACTIVA', ? FROM usuario WHERE correo_institucional = ?
                    """, idPractica, codificador.encode(codigo.getValue()), inicio, codigo.getKey());
        }
        log.info("Sesión de prueba de Ecos sembrada para Carlos Gómez con Ana y Luis.");
    }

    /** Los servicios de casos exigen un usuario autenticado: se actúa como el docente de prueba durante la siembra. */
    private static void actuarComo(long docenteId) {
        Jwt jwt = Jwt.withTokenValue("semilla").header("alg", "none").subject(String.valueOf(docenteId))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
