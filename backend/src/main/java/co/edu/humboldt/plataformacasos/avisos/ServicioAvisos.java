package co.edu.humboldt.plataformacasos.avisos;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Avisos dentro de la plataforma y registro de auditoría. Cualquier módulo puede dejar constancia de una acción
 * importante con {@link #registrar} y avisar a quien corresponda con las variantes de {@code avisar}.
 * Se ejecuta en su propia transacción para que un fallo del módulo que avisa nunca deje un aviso a medias
 * y para que los intentos fallidos de ingreso queden guardados aunque la petición termine en error.
 */
@Service
public class ServicioAvisos {

    private static final int MAX_TITULO = 150;
    private static final int MAX_TEXTO = 500;

    private static final Logger LOG = LoggerFactory.getLogger(ServicioAvisos.class);

    private final JdbcTemplate jdbc;
    private final Clock reloj;
    private final JavaMailSender correo;
    private final String remitente;
    private final boolean correoActivo;
    /** Un solo hilo: el envío de correos nunca frena la petición ni se atropella consigo mismo. */
    private final ExecutorService envios = Executors.newSingleThreadExecutor(r -> {
        Thread h = new Thread(r, "avisos-correo");
        h.setDaemon(true);
        return h;
    });

    public ServicioAvisos(JdbcTemplate jdbc, Clock reloj, JavaMailSender correo,
                          @Value("${app.mail.from:no-reply@institucion.edu.co}") String remitente,
                          @Value("${app.avisos.correo:true}") boolean correoActivo) {
        this.jdbc = jdbc;
        this.reloj = reloj;
        this.correo = correo;
        this.remitente = remitente;
        this.correoActivo = correoActivo;
    }

    @PreDestroy
    void cerrar() {
        envios.shutdown();
    }

    /** Deja constancia de una acción importante en la auditoría. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, Long actorId, String actorCorreo, String detalle) {
        jdbc.update("INSERT INTO auditoria (fecha, actor_id, actor_correo, accion, detalle) VALUES (?, ?, ?, ?, ?)",
                ahora(), actorId, recortar(actorCorreo, 150), recortar(accion, 60), recortar(detalle, MAX_TEXTO));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void avisar(Collection<Long> usuarios, String tipo, String titulo, String mensaje) {
        for (Long id : usuarios.stream().distinct().toList()) {
            jdbc.update("INSERT INTO aviso (usuario_id, tipo, titulo, mensaje, leido, fecha) VALUES (?, ?, ?, ?, FALSE, ?)",
                    id, recortar(tipo, 40), recortar(titulo, MAX_TITULO), recortar(mensaje, MAX_TEXTO), ahora());
            enviarCorreo(id, titulo, mensaje);
        }
    }

    /** Además del aviso en la campana, llega al correo institucional. Si el envío falla solo queda en el registro. */
    private void enviarCorreo(Long usuarioId, String titulo, String mensaje) {
        if (!correoActivo || correo == null) { return; }
        List<String> destinos = jdbc.queryForList("SELECT correo_institucional FROM usuario WHERE id = ? AND activo = TRUE", String.class, usuarioId);
        if (destinos.isEmpty()) { return; }
        String para = destinos.get(0);
        envios.submit(() -> {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                m.setFrom(remitente);
                m.setTo(para);
                m.setSubject("SOMA · " + titulo);
                m.setText(mensaje + "\n\n— Plataforma SOMA");
                correo.send(m);
            } catch (RuntimeException e) {
                LOG.warn("No se pudo enviar el aviso por correo a {}: {}", para, e.getMessage());
            }
        });
    }

    public List<Long> administradores() {
        return jdbc.queryForList("SELECT id FROM usuario WHERE rol = 'ADMINISTRADOR' AND activo = TRUE", Long.class);
    }

    /** Docentes que tienen al estudiante convocado en alguna sesión. */
    public List<Long> docentesDelEstudiante(Long idEstudiante) {
        return jdbc.queryForList("""
                SELECT DISTINCT p.id_docente FROM autorizaciones a JOIN practicas p ON p.id_practica = a.id_practica
                 WHERE a.id_estudiante = ? AND a.estado = 'ACTIVA'""", Long.class, idEstudiante);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void avisarAdministradores(String tipo, String titulo, String mensaje) {
        avisar(administradores(), tipo, titulo, mensaje);
    }

    @Transactional(readOnly = true)
    public List<AvisoDto> listar(Long usuarioId, int limite) {
        return jdbc.query("SELECT id, tipo, titulo, mensaje, leido, fecha FROM aviso WHERE usuario_id = ? ORDER BY id DESC LIMIT ?",
                (rs, n) -> new AvisoDto(rs.getLong("id"), rs.getString("tipo"), rs.getString("titulo"), rs.getString("mensaje"),
                        rs.getBoolean("leido"), rs.getTimestamp("fecha").toLocalDateTime()),
                usuarioId, Math.max(1, Math.min(limite, 100)));
    }

    @Transactional(readOnly = true)
    public int noLeidos(Long usuarioId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM aviso WHERE usuario_id = ? AND leido = FALSE", Integer.class, usuarioId);
        return n == null ? 0 : n;
    }

    @Transactional
    public void marcarLeido(Long usuarioId, Long idAviso) {
        jdbc.update("UPDATE aviso SET leido = TRUE WHERE id = ? AND usuario_id = ?", idAviso, usuarioId);
    }

    @Transactional
    public void marcarTodosLeidos(Long usuarioId) {
        jdbc.update("UPDATE aviso SET leido = TRUE WHERE usuario_id = ? AND leido = FALSE", usuarioId);
    }

    @Transactional(readOnly = true)
    public List<EntradaAuditoriaDto> auditoria(int limite) {
        return jdbc.query("SELECT id, fecha, actor_correo, accion, detalle FROM auditoria ORDER BY id DESC LIMIT ?",
                (rs, n) -> new EntradaAuditoriaDto(rs.getLong("id"), rs.getTimestamp("fecha").toLocalDateTime(),
                        rs.getString("actor_correo"), rs.getString("accion"), rs.getString("detalle")),
                Math.max(1, Math.min(limite, 500)));
    }

    private Timestamp ahora() {
        return Timestamp.valueOf(LocalDateTime.now(reloj));
    }

    private static String recortar(String s, int max) {
        if (s == null) { return null; }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
