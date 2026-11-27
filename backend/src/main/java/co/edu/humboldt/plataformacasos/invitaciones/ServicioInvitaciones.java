package co.edu.humboldt.plataformacasos.invitaciones;

import co.edu.humboldt.plataformacasos.ecos.EcosException;
import co.edu.humboldt.plataformacasos.invitaciones.ModeloInvitaciones.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * El docente invita a un estudiante por correo; quien abre el enlace crea su cuenta (el correo sale de la invitación,
 * no se escribe). Retirar a un estudiante de un docente borra solo el vínculo, nunca la cuenta.
 */
@Service
public class ServicioInvitaciones {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioInvitaciones.class);
    private static final Pattern CORREO = Pattern.compile("^[a-z0-9._%+\\-]+@[a-z0-9.\\-]+\\.[a-z]{2,}$");
    private static final Pattern NUMERO_DOCUMENTO = Pattern.compile("^[A-Za-z0-9]{5,15}$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Set<String> TIPOS_DOCUMENTO = Set.of("CC", "TI", "CE", "PAS", "PEP");
    private static final int MAX_CORREOS = 50;

    private final JdbcTemplate jdbc;
    private final Clock reloj;
    private final PropiedadesInvitaciones propiedades;
    private final PasswordEncoder codificador;
    private final JavaMailSender correo;
    private final java.util.List<String> dominios;
    private final String dominio;
    private final String remitente;
    private final SecureRandom azar = new SecureRandom();

    public ServicioInvitaciones(JdbcTemplate jdbc, Clock reloj, PropiedadesInvitaciones propiedades, PasswordEncoder codificador,
                                JavaMailSender correo, @Value("${app.autenticacion.dominio-institucional}") String dominio,
                                @Value("${app.mail.from:no-reply@institucion.edu.co}") String remitente) {
        this.jdbc = jdbc;
        this.reloj = reloj;
        this.propiedades = propiedades;
        this.codificador = codificador;
        this.correo = correo;
        this.dominios = java.util.Arrays.stream(dominio.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(d -> !d.isEmpty()).toList();
        this.dominio = String.join(" o @", dominios);
        this.remitente = remitente;
    }

    // ===================================================================== docente

    public List<ResultadoInvitacion> invitar(long docenteId, List<String> correos) {
        if (correos == null || correos.isEmpty()) {
            throw new EcosException(HttpStatus.BAD_REQUEST, "VALIDACION", "Escribe al menos un correo.");
        }
        Set<String> limpios = new LinkedHashSet<>();
        for (String c : correos) {
            if (c != null && !c.isBlank()) {
                limpios.add(c.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (limpios.isEmpty() || limpios.size() > MAX_CORREOS) {
            throw new EcosException(HttpStatus.BAD_REQUEST, "VALIDACION", "Envía entre 1 y " + MAX_CORREOS + " correos por vez.");
        }
        String docente = jdbc.queryForObject("SELECT nombre_completo FROM usuario WHERE id = ?", String.class, docenteId);
        List<ResultadoInvitacion> salida = new ArrayList<>();
        for (String c : limpios) {
            salida.add(invitarUno(docenteId, docente, c));
        }
        return salida;
    }

    private ResultadoInvitacion invitarUno(long docenteId, String docente, String correoEstudiante) {
        if (!CORREO.matcher(correoEstudiante).matches()) {
            return rechazo(correoEstudiante, "CORREO_INVALIDO", "No parece un correo válido.");
        }
        if (dominios.stream().noneMatch(d -> correoEstudiante.endsWith("@" + d))) {
            return rechazo(correoEstudiante, "DOMINIO_INVALIDO", "Debe ser un correo institucional (@" + dominio + ").");
        }
        List<Map<String, Object>> existente = jdbc.queryForList("SELECT id, rol FROM usuario WHERE correo_institucional = ?", correoEstudiante);
        if (!existente.isEmpty()) {
            if (!"ESTUDIANTE".equals(existente.get(0).get("rol"))) {
                return rechazo(correoEstudiante, "NO_ES_ESTUDIANTE", "Ese correo pertenece a personal de la plataforma.");
            }
            long estudianteId = ((Number) existente.get(0).get("id")).longValue();
            boolean nuevo = vincular(docenteId, estudianteId);
            String texto = nuevo ? "Ya tenía cuenta: quedó agregado a tus estudiantes." : "Ya estaba en tus estudiantes.";
            if (nuevo) {
                enviar(correoEstudiante, "Un docente te agregó en SOMA", "Ya estás en la lista de un docente",
                        List.of(docente + " te agregó a sus estudiantes en SOMA.", "Ingresa con tu cuenta de siempre para ver tus actividades."),
                        "Entrar a SOMA", propiedades.urlFrontend().replaceAll("/+$", "") + "/login",
                        "Si no esperabas este correo, puedes ignorarlo.");
            }
            return new ResultadoInvitacion(correoEstudiante, "VINCULADO", texto, null, nuevo);
        }
        return crearOReenviar(docenteId, docente, correoEstudiante);
    }

    private ResultadoInvitacion crearOReenviar(long docenteId, String docente, String correoEstudiante) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String token = nuevoToken();
        List<Long> pendientes = jdbc.queryForList(
                "SELECT id FROM invitacion_estudiante WHERE docente_id = ? AND correo = ? AND estado = 'PENDIENTE'", Long.class, docenteId, correoEstudiante);
        String resultado;
        if (pendientes.isEmpty()) {
            jdbc.update("INSERT INTO invitacion_estudiante (docente_id, correo, token_hash, estado, creada_en, vence_en) VALUES (?,?,?,'PENDIENTE',?,?)",
                    docenteId, correoEstudiante, hash(token), Timestamp.valueOf(ahora), Timestamp.valueOf(ahora.plusDays(propiedades.diasVigencia())));
            resultado = "ENVIADA";
        } else {
            jdbc.update("UPDATE invitacion_estudiante SET token_hash = ?, creada_en = ?, vence_en = ? WHERE id = ?",
                    hash(token), Timestamp.valueOf(ahora), Timestamp.valueOf(ahora.plusDays(propiedades.diasVigencia())), pendientes.get(0));
            resultado = "REENVIADA";
        }
        String enlace = enlace(token);
        String vigencia = propiedades.diasVigencia() + " días";
        boolean enviado = enviar(correoEstudiante, "Te invitaron a SOMA", "Te invitaron a SOMA",
                List.of(docente + " te invitó a SOMA, la plataforma de simulaciones para el aprendizaje en psicología social.",
                        "Crea tu cuenta con este mismo correo: elige tu contraseña y completa tus datos. El enlace es personal y vale " + vigencia + "."),
                "Crear mi cuenta", enlace, "Si no esperabas este correo, puedes ignorarlo: no se crea ninguna cuenta hasta que abras el enlace.");
        String texto = enviado ? ("ENVIADA".equals(resultado) ? "Invitación enviada." : "Invitación reenviada con un enlace nuevo.")
                : "La invitación quedó creada, pero el correo no se pudo enviar. Comparte el enlace tú mismo.";
        return new ResultadoInvitacion(correoEstudiante, resultado, texto, enlace, enviado);
    }

    @Transactional(readOnly = true)
    public List<InvitacionDocente> listar(long docenteId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return jdbc.query("""
                SELECT i.id, i.correo, i.estado, i.creada_en, i.vence_en, i.aceptada_en, u.nombre_completo
                  FROM invitacion_estudiante i LEFT JOIN usuario u ON u.id = i.estudiante_id
                 WHERE i.docente_id = ? ORDER BY i.creada_en DESC LIMIT 300
                """, (rs, n) -> {
            LocalDateTime vence = rs.getTimestamp(5).toLocalDateTime();
            String estado = rs.getString(3);
            if ("PENDIENTE".equals(estado) && vence.isBefore(ahora)) {
                estado = "VENCIDA";
            }
            return new InvitacionDocente(rs.getLong(1), rs.getString(2), estado, rs.getTimestamp(4).toLocalDateTime(), vence,
                    rs.getTimestamp(6) == null ? null : rs.getTimestamp(6).toLocalDateTime(), rs.getString(7));
        }, docenteId);
    }

    @Transactional
    public void cancelar(long docenteId, long invitacionId) {
        int n = jdbc.update("UPDATE invitacion_estudiante SET estado = 'CANCELADA' WHERE id = ? AND docente_id = ? AND estado = 'PENDIENTE'",
                invitacionId, docenteId);
        if (n == 0) {
            throw new EcosException(HttpStatus.NOT_FOUND, "INVITACION_NO_EXISTE", "No hay una invitación pendiente con ese número.");
        }
    }

    public ResultadoInvitacion reenviar(long docenteId, long invitacionId) {
        List<String> correoDe = jdbc.queryForList(
                "SELECT correo FROM invitacion_estudiante WHERE id = ? AND docente_id = ? AND estado = 'PENDIENTE'", String.class, invitacionId, docenteId);
        if (correoDe.isEmpty()) {
            throw new EcosException(HttpStatus.NOT_FOUND, "INVITACION_NO_EXISTE", "No hay una invitación pendiente con ese número.");
        }
        String docente = jdbc.queryForObject("SELECT nombre_completo FROM usuario WHERE id = ?", String.class, docenteId);
        return crearOReenviar(docenteId, docente, correoDe.get(0));
    }

    // ===================================================================== estudiante (público, con el token)

    @Transactional(readOnly = true)
    public InvitacionPublica consultar(String token) {
        Map<String, Object> i = invitacionVigente(token);
        return new InvitacionPublica((String) i.get("correo"), (String) i.get("docente"), aFecha(i.get("vence_en")));
    }

    @Transactional
    public CuentaCreada aceptar(String token, DatosRegistro d) {
        Map<String, Object> i = invitacionVigente(token);
        String correoEstudiante = (String) i.get("correo");
        validar(d, correoEstudiante);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE correo_institucional = ?", Integer.class, correoEstudiante) > 0) {
            throw new EcosException(HttpStatus.CONFLICT, "CUENTA_EXISTENTE", "Ese correo ya tiene una cuenta. Inicia sesión.");
        }
        long id;
        try {
            jdbc.update("INSERT INTO usuario (correo_institucional, nombre_completo, rol, activo, clave_hash, fecha_creacion) VALUES (?,?,'ESTUDIANTE',TRUE,?,?)",
                    correoEstudiante, d.nombreCompleto().trim().replaceAll("\\s+", " "), codificador.encode(d.contrasena()), Timestamp.valueOf(ahora));
            id = jdbc.queryForObject("SELECT id FROM usuario WHERE correo_institucional = ?", Long.class, correoEstudiante);
            jdbc.update("INSERT INTO estudiante_perfil (usuario_id, tipo_documento, numero_documento, fecha_nacimiento, telefono, acepto_datos_en, creado_en) VALUES (?,?,?,?,?,?,?)",
                    id, d.tipoDocumento().toUpperCase(Locale.ROOT), d.numeroDocumento().trim().toUpperCase(Locale.ROOT), d.fechaNacimiento(),
                    d.telefono() == null || d.telefono().isBlank() ? null : d.telefono().trim(), Timestamp.valueOf(ahora), Timestamp.valueOf(ahora));
        } catch (DuplicateKeyException e) {
            throw new EcosException(HttpStatus.CONFLICT, "DOCUMENTO_YA_REGISTRADO", "Ese documento ya está registrado en otra cuenta.");
        }
        // Esta invitación y cualquier otra pendiente para el mismo correo (de otros docentes) quedan aceptadas.
        for (Map<String, Object> p : jdbc.queryForList(
                "SELECT id, docente_id FROM invitacion_estudiante WHERE correo = ? AND estado = 'PENDIENTE' AND vence_en > ?", correoEstudiante, Timestamp.valueOf(ahora))) {
            jdbc.update("UPDATE invitacion_estudiante SET estado = 'ACEPTADA', aceptada_en = ?, estudiante_id = ? WHERE id = ?",
                    Timestamp.valueOf(ahora), id, p.get("id"));
            vincular(((Number) p.get("docente_id")).longValue(), id);
        }
        return new CuentaCreada(correoEstudiante, "Tu cuenta quedó creada. Ya puedes iniciar sesión.");
    }

    private void validar(DatosRegistro d, String correoEstudiante) {
        if (d == null) {
            throw new EcosException(HttpStatus.BAD_REQUEST, "VALIDACION", "Faltan los datos de registro.");
        }
        if (d.nombreCompleto() == null || d.nombreCompleto().trim().split("\\s+").length < 2 || d.nombreCompleto().trim().length() > 150) {
            throw invalido("Escribe tu nombre completo (nombre y apellido).");
        }
        if (d.tipoDocumento() == null || !TIPOS_DOCUMENTO.contains(d.tipoDocumento().toUpperCase(Locale.ROOT))) {
            throw invalido("Elige un tipo de documento válido.");
        }
        if (d.numeroDocumento() == null || !NUMERO_DOCUMENTO.matcher(d.numeroDocumento().trim()).matches()) {
            throw invalido("El número de documento debe tener entre 5 y 15 letras o números.");
        }
        if (d.fechaNacimiento() == null || d.fechaNacimiento().isAfter(LocalDate.now(reloj))) {
            throw invalido("La fecha de nacimiento no es válida.");
        }
        int edad = Period.between(d.fechaNacimiento(), LocalDate.now(reloj)).getYears();
        if (edad < propiedades.edadMinima() || edad > 100) {
            throw invalido("La edad debe estar entre " + propiedades.edadMinima() + " y 100 años.");
        }
        if (d.telefono() != null && !d.telefono().isBlank() && !TELEFONO.matcher(d.telefono().trim()).matches()) {
            throw invalido("El teléfono debe tener entre 7 y 15 dígitos (puede empezar por +).");
        }
        if (!Boolean.TRUE.equals(d.aceptaTratamientoDatos())) {
            throw invalido("Debes aceptar el tratamiento de tus datos personales para crear la cuenta.");
        }
        String c = d.contrasena();
        if (c == null || c.length() < 8 || c.length() > 72 || !c.matches(".*[a-z].*") || !c.matches(".*[A-Z].*") || !c.matches(".*[0-9].*")) {
            throw invalido("La contraseña debe tener entre 8 y 72 caracteres, con mayúscula, minúscula y número.");
        }
        if (c.toLowerCase(Locale.ROOT).contains(correoEstudiante.substring(0, correoEstudiante.indexOf('@')).toLowerCase(Locale.ROOT))) {
            throw invalido("La contraseña no puede contener tu correo.");
        }
    }

    /** MySQL entrega LocalDateTime y H2 entrega Timestamp: se aceptan ambos. */
    private static LocalDateTime aFecha(Object valor) {
        return valor instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) valor;
    }

    /** Busca la invitación por el hash del token y explica por qué no sirve si no sirve. */
    private Map<String, Object> invitacionVigente(String token) {
        List<Map<String, Object>> f = jdbc.queryForList("""
                SELECT i.id, i.correo, i.estado, i.vence_en, u.nombre_completo AS docente
                  FROM invitacion_estudiante i JOIN usuario u ON u.id = i.docente_id WHERE i.token_hash = ?
                """, hash(token == null ? "" : token));
        if (f.isEmpty()) {
            throw new EcosException(HttpStatus.NOT_FOUND, "INVITACION_NO_EXISTE", "Este enlace de invitación no es válido.");
        }
        Map<String, Object> i = f.get(0);
        String estado = (String) i.get("estado");
        if ("ACEPTADA".equals(estado)) {
            throw new EcosException(HttpStatus.GONE, "INVITACION_USADA", "Esta invitación ya se usó. Inicia sesión con tu cuenta.");
        }
        if ("CANCELADA".equals(estado)) {
            throw new EcosException(HttpStatus.GONE, "INVITACION_CANCELADA", "Tu docente canceló esta invitación. Pídele una nueva.");
        }
        if (aFecha(i.get("vence_en")).isBefore(LocalDateTime.now(reloj))) {
            throw new EcosException(HttpStatus.GONE, "INVITACION_VENCIDA", "Esta invitación venció. Pídele a tu docente que te la reenvíe.");
        }
        return i;
    }

    // ===================================================================== apoyo

    /** @return true si el vínculo se creó (false si ya existía). */
    boolean vincular(long docenteId, long estudianteId) {
        return jdbc.update("INSERT INTO docente_estudiante (docente_id, estudiante_id, vinculado_en) "
                        + "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM docente_estudiante WHERE docente_id = ? AND estudiante_id = ?)",
                docenteId, estudianteId, Timestamp.valueOf(LocalDateTime.now(reloj)), docenteId, estudianteId) > 0;
    }

    private boolean enviar(String para, String asunto, String titulo, List<String> parrafos, String textoBoton, String enlace, String nota) {
        try {
            var m = correo.createMimeMessage();
            var h = new MimeMessageHelper(m, true, "UTF-8");
            h.setFrom(remitente);
            h.setTo(para);
            h.setSubject("SOMA · " + asunto);
            String plano = titulo + "\n\n" + String.join("\n\n", parrafos) + "\n\n" + textoBoton + ": " + enlace + "\n\n" + nota + "\n\n— Plataforma SOMA";
            h.setText(plano, PlantillaCorreo.html(titulo, parrafos, textoBoton, enlace, nota));
            correo.send(m);
            return true;
        } catch (Exception e) {
            LOG.warn("No se pudo enviar el correo a {}: {}", para, e.getMessage());
            return false;
        }
    }

    private String enlace(String token) {
        String base = propiedades.urlFrontend().replaceAll("/+$", "");
        return base + "/registro/" + token;
    }

    private String nuevoToken() {
        byte[] b = new byte[32];
        azar.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ResultadoInvitacion rechazo(String correoEstudiante, String codigo, String mensaje) {
        return new ResultadoInvitacion(correoEstudiante, codigo, mensaje, null, false);
    }

    private static EcosException invalido(String mensaje) {
        return new EcosException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACION", mensaje);
    }
}
