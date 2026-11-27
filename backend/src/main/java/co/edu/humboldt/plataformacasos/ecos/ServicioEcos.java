package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Sesiones de juego, canje, avance y resultado de Ecos de una decisión (contrato ecos-soma/1).
 * Regla de oro: se guarda la nota que calcula SOMA con las respuestas recibidas; la del juego solo se compara.
 */
@Service
public class ServicioEcos {

    /** Letras sin I ni O (se confunden con 1 y 0) y dígitos 2-9: siempre dentro de [A-Z2-9]. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern CODIGO = Pattern.compile("^[A-Z2-9]{4}-[A-Z2-9]{4}$");
    private static final Pattern VERSION = Pattern.compile("^[0-9a-f]{32}$");
    private static final Set<String> PROTAGONISTAS = Set.of("Andrea", "Mateo");
    private static final Set<String> DESENLACES = Set.of("FIN_SOSTENIDA", "FIN_FRAGIL", "FIN_ROTA");

    private record Fila(String id, String estudianteId, String casoVersion, int intento, String estado,
                        LocalDateTime canjeadaEn, LocalDateTime venceEn) {
    }

    private final JdbcTemplate jdbc;
    private final Clock reloj;
    private final PropiedadesEcos propiedades;
    private final TokenEcos tokens;
    private final ObjectMapper json;
    private final SecureRandom azar = new SecureRandom();

    public ServicioEcos(JdbcTemplate jdbc, Clock reloj, PropiedadesEcos propiedades, TokenEcos tokens, ObjectMapper json) {
        this.jdbc = jdbc;
        this.reloj = reloj;
        this.propiedades = propiedades;
        this.tokens = tokens;
        this.json = json;
    }

    // ===================================================================== catálogo

    /** @return true si se creó; false si esa versión ya estaba cargada (no se toca nada). */
    @Transactional
    public boolean importarCatalogo(CatalogoImportado c) {
        validarCatalogo(c);
        String version = c.casoVersion().toLowerCase();
        if (jdbc.queryForObject("SELECT COUNT(*) FROM ecos_caso WHERE caso_version = ?", Integer.class, version) > 0) {
            return false;
        }
        jdbc.update("INSERT INTO ecos_caso (caso_version, caso_id, nombre, credito_parcial, cargado_en) VALUES (?,?,?,?,?)",
                version, c.casoId(), c.nombre(), c.creditoParcial(), ahora());
        for (CriterioImportado k : c.criterios()) {
            jdbc.update("INSERT INTO ecos_criterio (caso_version, criterio_id, nombre) VALUES (?,?,?)", version, k.id(), k.nombre());
        }
        int orden = 0;
        for (PreguntaImportada p : c.preguntas()) {
            jdbc.update("INSERT INTO ecos_pregunta (caso_version, pregunta_id, orden, escenario, nucleo, puntos, enunciado) VALUES (?,?,?,?,?,?,?)",
                    version, p.id(), orden++, p.escenario(), p.nucleo(), p.puntos(), p.enunciado());
            for (OpcionImportada o : p.opciones()) {
                jdbc.update("INSERT INTO ecos_opcion (caso_version, pregunta_id, opcion_id, grado, credito, texto, retroalimentacion) VALUES (?,?,?,?,?,?,?)",
                        version, p.id(), o.id(), o.grado(), o.credito(), o.texto(), o.retroalimentacion());
            }
            for (String k : p.criterios() == null ? List.<String>of() : p.criterios()) {
                jdbc.update("INSERT INTO ecos_pregunta_criterio (caso_version, pregunta_id, criterio_id) VALUES (?,?,?)", version, p.id(), k);
            }
        }
        return true;
    }

    private void validarCatalogo(CatalogoImportado c) {
        if (c == null || c.casoVersion() == null || !VERSION.matcher(c.casoVersion().toLowerCase()).matches()) {
            throw new SolicitudInvalidaException("casoVersion debe ser el MD5 del banco de preguntas (32 caracteres hexadecimales).");
        }
        if (vacio(c.casoId()) || vacio(c.nombre()) || c.preguntas() == null || c.preguntas().isEmpty()
                || c.criterios() == null) {
            throw new SolicitudInvalidaException("El catálogo necesita casoId, nombre, criterios y al menos una pregunta.");
        }
        Set<String> criterios = new HashSet<>();
        for (CriterioImportado k : c.criterios()) {
            if (k == null || vacio(k.id()) || vacio(k.nombre()) || !criterios.add(k.id())) {
                throw new SolicitudInvalidaException("Criterio inválido o repetido.");
            }
        }
        Set<String> preguntas = new HashSet<>();
        for (PreguntaImportada p : c.preguntas()) {
            if (p == null || vacio(p.id()) || p.id().length() > 16 || !preguntas.add(p.id()) || vacio(p.escenario())
                    || vacio(p.enunciado()) || p.puntos() == null || p.puntos().signum() <= 0
                    || p.opciones() == null || p.opciones().size() < 2) {
                throw new SolicitudInvalidaException("Pregunta inválida o repetida: " + (p == null ? "?" : p.id()));
            }
            for (String k : p.criterios() == null ? List.<String>of() : p.criterios()) {
                if (!criterios.contains(k)) {
                    throw new SolicitudInvalidaException("La pregunta " + p.id() + " usa el criterio inexistente " + k + ".");
                }
            }
            Set<String> opciones = new HashSet<>();
            for (OpcionImportada o : p.opciones()) {
                if (o == null || vacio(o.id()) || o.id().length() > 8 || !opciones.add(o.id()) || vacio(o.texto())
                        || o.credito() == null || o.grado() == null) {
                    throw new SolicitudInvalidaException("Opción inválida o repetida en " + p.id() + ".");
                }
                boolean coherente = switch (o.grado()) {
                    case "Correct" -> o.credito().compareTo(BigDecimal.ONE) == 0;
                    case "Incorrect" -> o.credito().signum() == 0;
                    case "Partial" -> o.credito().signum() > 0 && o.credito().compareTo(BigDecimal.ONE) < 0;
                    default -> false;
                };
                if (!coherente) {
                    throw new SolicitudInvalidaException("Grado o crédito incoherente en " + p.id() + "/" + o.id()
                            + " (Correct=1, Partial entre 0 y 1, Incorrect=0).");
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<CasoCatalogo> cargar(String version) {
        List<Map<String, Object>> caso = jdbc.queryForList("SELECT caso_id, nombre FROM ecos_caso WHERE caso_version = ?", version);
        if (caso.isEmpty()) {
            return Optional.empty();
        }
        Map<String, List<String>> criteriosDe = new LinkedHashMap<>();
        jdbc.query("SELECT pregunta_id, criterio_id FROM ecos_pregunta_criterio WHERE caso_version = ? ORDER BY criterio_id",
                rs -> { criteriosDe.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2)); }, version);
        Map<String, Map<String, Opcion>> opcionesDe = new LinkedHashMap<>();
        jdbc.query("SELECT pregunta_id, opcion_id, grado, credito FROM ecos_opcion WHERE caso_version = ?",
                rs -> { opcionesDe.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>())
                        .put(rs.getString(2), new Opcion(rs.getString(2), rs.getString(3), rs.getBigDecimal(4))); }, version);
        List<Pregunta> preguntas = jdbc.query("SELECT pregunta_id, puntos FROM ecos_pregunta WHERE caso_version = ? ORDER BY orden",
                (rs, n) -> new Pregunta(rs.getString(1), rs.getBigDecimal(2), criteriosDe.getOrDefault(rs.getString(1), List.of()),
                        opcionesDe.getOrDefault(rs.getString(1), Map.of())), version);
        List<String> criterios = jdbc.queryForList("SELECT criterio_id FROM ecos_criterio WHERE caso_version = ? ORDER BY criterio_id", String.class, version);
        return Optional.of(new CasoCatalogo(version, (String) caso.get(0).get("caso_id"), (String) caso.get(0).get("nombre"), preguntas, criterios));
    }

    private Optional<String> versionVigente() {
        List<String> v = jdbc.queryForList("SELECT caso_version FROM ecos_caso ORDER BY cargado_en DESC, caso_version LIMIT 1", String.class);
        return v.stream().findFirst();
    }

    // ===================================================================== estudiante (JWT web)

    @Transactional
    public SesionCreada crearSesion(Long usuarioId) {
        String estudiante = String.valueOf(usuarioId);
        String version = versionVigente().orElseThrow(() ->
                new EcosException(HttpStatus.CONFLICT, "SIN_CATALOGO", "El caso Ecos de una decisión aún no está disponible."));
        String casoId = jdbc.queryForObject("SELECT caso_id FROM ecos_caso WHERE caso_version = ?", String.class, version);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (ocupados(estudiante, casoId, "", ahora) >= maximo(estudiante)) {
            throw new EcosException(HttpStatus.LOCKED, "INTENTOS_AGOTADOS", "Ya usaste todos tus intentos.");
        }
        // Un solo código válido por estudiante: los anteriores sin canjear dejan de servir.
        jdbc.update("UPDATE ecos_sesion SET estado = 'VENCIDA' WHERE estudiante_id = ? AND estado = 'CREADA'", estudiante);
        int intento = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ecos_sesion s JOIN ecos_caso c ON c.caso_version = s.caso_version
                 WHERE s.estudiante_id = ? AND c.caso_id = ? AND s.estado = 'CERRADA'
                """, Integer.class, estudiante, casoId) + 1;
        String sesionId = UUID.randomUUID().toString();
        LocalDateTime vence = ahora.plusMinutes(propiedades.minutosCodigo());
        for (int i = 0; ; i++) {
            String codigo = nuevoCodigo();
            try {
                jdbc.update("INSERT INTO ecos_sesion (sesion_id, codigo, estudiante_id, caso_version, intento, estado, creada_en, vence_en) VALUES (?,?,?,?,?,'CREADA',?,?)",
                        sesionId, codigo, estudiante, version, intento, Timestamp.valueOf(ahora), Timestamp.valueOf(vence));
                String enlace = "ecos://iniciar?sesion=" + codigo + "&api=" + propiedades.apiPublica();
                return new SesionCreada(sesionId, codigo, enlace, vence, intento);
            } catch (DuplicateKeyException repetido) {
                if (i >= 5) {
                    throw repetido;
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public EstadoEstudiante estado(Long usuarioId) {
        String estudiante = String.valueOf(usuarioId);
        int max = maximo(estudiante);
        Optional<String> version = versionVigente();
        if (version.isEmpty()) {
            return new EstadoEstudiante(null, false, max, 0, max, List.of());
        }
        String casoId = jdbc.queryForObject("SELECT caso_id FROM ecos_caso WHERE caso_version = ?", String.class, version.get());
        String nombre = jdbc.queryForObject("SELECT nombre FROM ecos_caso WHERE caso_version = ?", String.class, version.get());
        int usados = ocupados(estudiante, casoId, "", LocalDateTime.now(reloj));
        List<IntentoEcos> intentos = jdbc.query("""
                SELECT s.sesion_id, s.intento, s.estado, s.creada_en, r.puntaje, r.nota, r.nivel, r.desenlace, r.fin
                  FROM ecos_sesion s LEFT JOIN ecos_resultado r ON r.sesion_id = s.sesion_id
                 WHERE s.estudiante_id = ? AND s.estado <> 'VENCIDA' ORDER BY s.creada_en DESC LIMIT 50
                """, (rs, n) -> new IntentoEcos(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getTimestamp(4).toLocalDateTime(),
                rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getString(7), rs.getString(8),
                rs.getTimestamp(9) == null ? null : rs.getTimestamp(9).toLocalDateTime(), false), estudiante);
        // Nota oficial = el mejor intento terminado; si empatan, el primero.
        IntentoEcos mejor = intentos.stream().filter(i -> i.nota() != null)
                .max(java.util.Comparator.comparing(IntentoEcos::nota).thenComparing(java.util.Comparator.comparingInt(IntentoEcos::intento).reversed()))
                .orElse(null);
        List<IntentoEcos> marcados = intentos.stream().map(i -> new IntentoEcos(i.sesionId(), i.intento(), i.estado(), i.creada(),
                i.puntaje(), i.nota(), i.nivel(), i.desenlace(), i.fin(), i == mejor)).toList();
        return new EstadoEstudiante(nombre, usados < max, max, usados, Math.max(0, max - usados), marcados);
    }

    /** Intentos permitidos al estudiante: los base de la configuración más una repetición por cada una que concedió un docente. */
    private int maximo(String estudiante) {
        Integer extra = jdbc.queryForObject("SELECT COUNT(*) FROM ecos_repeticion WHERE estudiante_id = ?", Integer.class, estudiante);
        return propiedades.maxIntentos() + (extra == null ? 0 : extra);
    }

    // ===================================================================== docente

    /** Resultados de Ecos de los estudiantes de este docente (los que lo aceptaron como docente). */
    @Transactional(readOnly = true)
    public List<ResultadoDocente> resultadosDelDocente(Long docenteId) {
        Map<String, List<String>> fortalezas = new LinkedHashMap<>();
        Map<String, List<String>> mejoras = new LinkedHashMap<>();
        jdbc.query("""
                SELECT rc.envio_id, rc.criterio_id, rc.tipo
                  FROM ecos_resultado_criterio rc
                  JOIN ecos_resultado r ON r.envio_id = rc.envio_id
                  JOIN ecos_sesion s ON s.sesion_id = r.sesion_id
                  JOIN docente_estudiante de ON CAST(de.estudiante_id AS CHAR) = s.estudiante_id AND de.docente_id = ?
                 WHERE rc.tipo IN ('FORTALEZA','MEJORA') ORDER BY rc.criterio_id""", rs -> {
            (rs.getString(3).equals("FORTALEZA") ? fortalezas : mejoras)
                    .computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
        }, docenteId);
        record Fila(ResultadoDocente r, String estudianteId) {
        }
        List<Fila> filas = jdbc.query("""
                SELECT r.envio_id, s.sesion_id, u.nombre_completo, s.intento, r.puntaje, r.nota, r.nivel, r.desenlace,
                       r.protagonista, r.segundos, r.difiere, r.fin, s.estudiante_id
                  FROM ecos_resultado r
                  JOIN ecos_sesion s ON s.sesion_id = r.sesion_id
                  JOIN usuario u ON CAST(u.id AS CHAR) = s.estudiante_id
                  JOIN docente_estudiante de ON de.estudiante_id = u.id AND de.docente_id = ?
                 ORDER BY r.fin DESC LIMIT 500""",
                (rs, n) -> new Fila(new ResultadoDocente(rs.getString(2), rs.getString(3), rs.getInt(4), rs.getBigDecimal(5), rs.getBigDecimal(6),
                        rs.getString(7), rs.getString(8), rs.getString(9), rs.getBigDecimal(10), rs.getBoolean(11),
                        rs.getTimestamp(12).toLocalDateTime(), fortalezas.getOrDefault(rs.getString(1), List.of()),
                        mejoras.getOrDefault(rs.getString(1), List.of()), false), rs.getString(13)), docenteId);
        // Oficial por estudiante: la mejor nota; si empatan, el primer intento.
        Map<String, Fila> mejorDe = new LinkedHashMap<>();
        for (Fila f : filas) {
            Fila actual = mejorDe.get(f.estudianteId());
            if (actual == null || f.r().nota().compareTo(actual.r().nota()) > 0
                    || (f.r().nota().compareTo(actual.r().nota()) == 0 && f.r().intento() < actual.r().intento())) {
                mejorDe.put(f.estudianteId(), f);
            }
        }
        return filas.stream().map(f -> {
            ResultadoDocente r = f.r();
            return new ResultadoDocente(r.sesionId(), r.estudiante(), r.intento(), r.puntaje(), r.nota(), r.nivel(), r.desenlace(), r.protagonista(),
                    r.segundos(), r.revision(), r.fin(), r.fortalezas(), r.mejoras(), mejorDe.get(f.estudianteId()) == f);
        }).toList();
    }

    /** Quién está jugando ahora: códigos sin abrir, partidas en curso y las terminadas hace menos de 15 minutos. */
    @Transactional(readOnly = true)
    public List<EnVivo> enVivo(Long docenteId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return jdbc.query("""
                SELECT s.sesion_id, u.nombre_completo, s.estado, s.intento, s.ultimo_punto, s.ultimo_acto, s.canjeada_en,
                       s.ultimo_evento_en, s.vence_en, r.nota, r.nivel
                  FROM ecos_sesion s
                  JOIN usuario u ON CAST(u.id AS CHAR) = s.estudiante_id
                  JOIN docente_estudiante de ON de.estudiante_id = u.id AND de.docente_id = ?
                  LEFT JOIN ecos_resultado r ON r.sesion_id = s.sesion_id
                 WHERE (s.estado = 'CREADA' AND s.vence_en > ?)
                    OR (s.estado = 'CANJEADA' AND s.canjeada_en > ?)
                    OR (s.estado = 'CERRADA' AND r.recibido_en > ?)
                 ORDER BY COALESCE(s.ultimo_evento_en, s.canjeada_en, s.creada_en) DESC LIMIT 100
                """, (rs, n) -> new EnVivo(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                rs.getObject(6) == null ? null : rs.getInt(6), fecha(rs.getTimestamp(7)), fecha(rs.getTimestamp(8)),
                fecha(rs.getTimestamp(9)), rs.getBigDecimal(10), rs.getString(11)),
                docenteId, Timestamp.valueOf(ahora), Timestamp.valueOf(ahora.minusHours(propiedades.horasToken())),
                Timestamp.valueOf(ahora.minusMinutes(15)));
    }

    @Transactional(readOnly = true)
    public List<EstudianteDocente> estudiantesDelDocente(Long docenteId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Timestamp vigenteDesde = Timestamp.valueOf(ahora.minusHours(propiedades.horasToken()));
        return jdbc.query("""
                SELECT u.id, u.nombre_completo, u.correo_institucional, de.vinculado_en,
                  (SELECT COUNT(*) FROM ecos_repeticion er WHERE er.estudiante_id = CAST(u.id AS CHAR)),
                  (SELECT COUNT(*) FROM ecos_sesion s WHERE s.estudiante_id = CAST(u.id AS CHAR)
                      AND (s.estado = 'CERRADA' OR (s.estado = 'CANJEADA' AND s.canjeada_en > ?))),
                  (SELECT MAX(r.nota) FROM ecos_resultado r JOIN ecos_sesion s ON s.sesion_id = r.sesion_id WHERE s.estudiante_id = CAST(u.id AS CHAR)),
                  (SELECT COUNT(*) FROM ecos_sesion s WHERE s.estudiante_id = CAST(u.id AS CHAR) AND s.estado = 'CANJEADA' AND s.canjeada_en > ?),
                  (SELECT COUNT(*) FROM ecos_sesion s WHERE s.estudiante_id = CAST(u.id AS CHAR) AND s.estado = 'CREADA' AND s.vence_en > ?)
                  FROM docente_estudiante de JOIN usuario u ON u.id = de.estudiante_id
                 WHERE de.docente_id = ? ORDER BY u.nombre_completo
                """, (rs, n) -> {
            BigDecimal mejor = rs.getBigDecimal(7);
            int usados = rs.getInt(6);
            String estado = rs.getInt(8) > 0 ? "JUGANDO" : rs.getInt(9) > 0 ? "CODIGO_GENERADO" : mejor != null ? "TERMINADO" : "SIN_JUGAR";
            int repeticiones = rs.getInt(5);
            return new EstudianteDocente(rs.getLong(1), rs.getString(2), rs.getString(3), fecha(rs.getTimestamp(4)), usados,
                    propiedades.maxIntentos() + repeticiones, repeticiones, mejor, mejor == null ? null : CalculoEcos.nivel(mejor), estado);
        }, vigenteDesde, vigenteDesde, Timestamp.valueOf(ahora), docenteId);
    }

    /** Retirar saca al estudiante de la lista de este docente; su cuenta y sus resultados no se tocan. */
    @Transactional
    public void retirarEstudiante(Long docenteId, long estudianteId) {
        if (jdbc.update("DELETE FROM docente_estudiante WHERE docente_id = ? AND estudiante_id = ?", docenteId, estudianteId) == 0) {
            throw new EcosException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO", "Ese estudiante no está en tu lista.");
        }
    }

    /** Un intento más para un estudiante del docente (por ejemplo, si el juego se cerró antes de terminar). */
    @Transactional
    public EstudianteDocente concederRepeticion(Long docenteId, long estudianteId) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM docente_estudiante WHERE docente_id = ? AND estudiante_id = ?", Integer.class, docenteId, estudianteId) == 0) {
            throw new EcosException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO", "Ese estudiante no está en tu lista.");
        }
        jdbc.update("INSERT INTO ecos_repeticion (estudiante_id, docente_id, concedida_en) VALUES (?,?,?)", String.valueOf(estudianteId), docenteId, ahora());
        return estudiantesDelDocente(docenteId).stream().filter(e -> e.id() == estudianteId).findFirst().orElseThrow();
    }

    private static LocalDateTime fecha(Timestamp t) {
        return t == null ? null : t.toLocalDateTime();
    }

    /** Intentos que cuentan: los cerrados y los canjeados que siguen vigentes (el token aún no venció). */
    private int ocupados(String estudiante, String casoId, String exceptoSesion, LocalDateTime ahora) {
        LocalDateTime limite = ahora.minusHours(propiedades.horasToken());
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM ecos_sesion s JOIN ecos_caso c ON c.caso_version = s.caso_version
                 WHERE s.estudiante_id = ? AND c.caso_id = ? AND s.sesion_id <> ?
                   AND (s.estado = 'CERRADA' OR (s.estado = 'CANJEADA' AND s.canjeada_en > ?))
                """, Integer.class, estudiante, casoId, exceptoSesion, Timestamp.valueOf(limite));
    }

    private String nuevoCodigo() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(ALFABETO.charAt(azar.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    // ===================================================================== juego (código y token de sesión)

    /** El código es la credencial: un solo uso y con vencimiento. Un rechazo por vencimiento sí se guarda. */
    @Transactional(noRollbackFor = EcosException.class)
    public RespuestaCanje canjear(String codigoCrudo) {
        String codigo = codigoCrudo == null ? "" : codigoCrudo.trim().toUpperCase();
        List<Fila> filas = CODIGO.matcher(codigo).matches()
                ? jdbc.query("SELECT sesion_id, estudiante_id, caso_version, intento, estado, canjeada_en, vence_en FROM ecos_sesion WHERE codigo = ?",
                        this::fila, codigo)
                : List.of();
        if (filas.isEmpty()) {
            throw new EcosException(HttpStatus.NOT_FOUND, "SESION_NO_EXISTE", "El código de sesión no existe.");
        }
        Fila s = filas.get(0);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if ("VENCIDA".equals(s.estado())) {
            throw new EcosException(HttpStatus.GONE, "SESION_VENCIDA", "El código de sesión venció. Genera uno nuevo desde SOMA.");
        }
        if (!"CREADA".equals(s.estado())) {
            throw new EcosException(HttpStatus.CONFLICT, "SESION_YA_USADA", "Ese código ya se usó.");
        }
        if (s.venceEn().isBefore(ahora)) {
            jdbc.update("UPDATE ecos_sesion SET estado = 'VENCIDA' WHERE sesion_id = ? AND estado = 'CREADA'", s.id());
            throw new EcosException(HttpStatus.GONE, "SESION_VENCIDA", "El código de sesión venció. Genera uno nuevo desde SOMA.");
        }
        String casoId = jdbc.queryForObject("SELECT caso_id FROM ecos_caso WHERE caso_version = ?", String.class, s.casoVersion());
        if (ocupados(s.estudianteId(), casoId, s.id(), ahora) >= maximo(s.estudianteId())) {
            throw new EcosException(HttpStatus.LOCKED, "INTENTOS_AGOTADOS", "Ya usaste todos tus intentos.");
        }
        int tomado = jdbc.update("UPDATE ecos_sesion SET estado = 'CANJEADA', canjeada_en = ? WHERE sesion_id = ? AND estado = 'CREADA'",
                Timestamp.valueOf(ahora), s.id());
        if (tomado == 0) {
            throw new EcosException(HttpStatus.CONFLICT, "SESION_YA_USADA", "Ese código ya se usó.");
        }
        String nombre = jdbc.queryForObject("SELECT nombre_completo FROM usuario WHERE id = ?", String.class, Long.valueOf(s.estudianteId()));
        return new RespuestaCanje(ModeloEcos.CONTRATO, s.id(), tokens.emitir(s.id(), s.estudianteId()),
                new EstudianteCanje(s.estudianteId(), nombreVisible(nombre)), new CasoRef(casoId, s.casoVersion()), s.intento(), false);
    }

    @Transactional
    public void registrarEvento(String sesionId, String encabezado, EventoJuego e) {
        Fila s = autenticar(sesionId, encabezado);
        if (!"CANJEADA".equals(s.estado())) {
            throw new EcosException(HttpStatus.CONFLICT, "SESION_CERRADA", "La sesión ya no admite eventos.");
        }
        if (e == null || vacio(e.punto()) || e.punto().length() > 16 || (e.acto() != null && (e.acto() < 0 || e.acto() > 9))) {
            throw new EcosException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACION", "Evento inválido.");
        }
        jdbc.update("UPDATE ecos_sesion SET ultimo_punto = ?, ultimo_acto = ?, ultimo_evento_en = ? WHERE sesion_id = ?", e.punto(), e.acto(), ahora(), sesionId);
    }

    /** @return true si se creó (201); false si ya estaba recibido (200, mismo envioId). */
    public record Recepcion(ResultadoRecibido resultado, boolean creado) {
    }

    @Transactional
    public Recepcion recibirResultado(String sesionId, String encabezado, EnvioResultado e) {
        Fila s = autenticar(sesionId, encabezado);
        if (e == null || e.envioId() == null || e.contrato() == null || !e.contrato().startsWith("ecos-soma/1")) {
            throw validacion("CONTRATO_NO_SOPORTADO", "El contrato debe ser ecos-soma/1.");
        }
        String envioId = uuid(e.envioId());
        Optional<ResultadoRecibido> previo = resultadoDe(envioId, s.id());
        if (previo.isPresent()) {
            return new Recepcion(previo.get(), false);
        }
        if (!sesionId.equals(e.sesionId())) {
            throw validacion("VALIDACION", "El sesionId del envío no coincide con el de la ruta.");
        }
        if (!"CANJEADA".equals(s.estado())) {
            throw new EcosException(HttpStatus.CONFLICT, "SESION_CERRADA", "La sesión ya está cerrada.");
        }
        if (e.caso() == null || !s.casoVersion().equals(e.caso().version())) {
            throw validacion("CASO_DISTINTO", "El banco de preguntas del juego no es el que SOMA tiene cargado.");
        }
        CasoCatalogo caso = cargar(s.casoVersion()).orElseThrow();
        Map<String, String> respuestas = validarRespuestas(caso, e.respuestas());
        validarMetadatos(s, e);

        // Cierra la sesión antes de guardar: si dos envíos llegan juntos, solo uno lo logra.
        int cerrada = jdbc.update("UPDATE ecos_sesion SET estado = 'CERRADA' WHERE sesion_id = ? AND estado = 'CANJEADA'", s.id());
        if (cerrada == 0) {
            return resultadoDe(envioId, s.id()).map(r -> new Recepcion(r, false)).orElseThrow(() ->
                    new EcosException(HttpStatus.CONFLICT, "SESION_CERRADA", "La sesión ya está cerrada."));
        }
        CalculoEcos.Calculo c = CalculoEcos.calcular(caso, respuestas);
        BigDecimal notaJuego = e.calculoJuego() == null || e.calculoJuego().nota() == null ? null
                : e.calculoJuego().nota().setScale(1, RoundingMode.HALF_UP);
        boolean difiere = notaJuego != null && notaJuego.compareTo(c.nota()) != 0;
        LocalDateTime inicio = e.inicio().atZoneSameInstant(reloj.getZone()).toLocalDateTime();
        LocalDateTime fin = e.fin().atZoneSameInstant(reloj.getZone()).toLocalDateTime();
        jdbc.update("""
                INSERT INTO ecos_resultado (envio_id, sesion_id, inicio, fin, segundos, protagonista, desenlace, puntaje, nota, nivel,
                                            nota_juego, difiere, indicadores, json_original, recibido_en)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, envioId, s.id(), Timestamp.valueOf(inicio), Timestamp.valueOf(fin), e.segundosParticipacion().setScale(2, RoundingMode.HALF_UP),
                e.protagonista(), e.desenlace(), c.puntaje(), c.nota(), c.nivel(), notaJuego, difiere,
                e.indicadores() == null ? null : json.writeValueAsString(e.indicadores()), json.writeValueAsString(e), ahora());
        for (CalculoEcos.Item i : c.items()) {
            jdbc.update("INSERT INTO ecos_respuesta (envio_id, pregunta_id, caso_version, opcion_id, obtenido, posible) VALUES (?,?,?,?,?,?)",
                    envioId, i.preguntaId(), s.casoVersion(), i.opcionId(), i.obtenido(), i.posible());
        }
        for (CalculoEcos.Criterio k : c.criterios()) {
            jdbc.update("INSERT INTO ecos_resultado_criterio (envio_id, criterio_id, caso_version, obtenido, posible, porcentaje, tipo) VALUES (?,?,?,?,?,?,?)",
                    envioId, k.id(), s.casoVersion(), k.obtenido(), k.posible(), k.porcentaje(), k.tipo());
        }
        return new Recepcion(resultadoDe(envioId, s.id()).orElseThrow(), true);
    }

    // ===================================================================== apoyo

    private Map<String, String> validarRespuestas(CasoCatalogo caso, List<RespuestaJuego> enviadas) {
        if (enviadas == null || enviadas.size() != caso.preguntas().size()) {
            throw validacion("VALIDACION", "Deben venir las " + caso.preguntas().size() + " preguntas, siempre todas.");
        }
        Map<String, String> mapa = new LinkedHashMap<>();
        for (RespuestaJuego r : enviadas) {
            if (r == null || r.preguntaId() == null || mapa.containsKey(r.preguntaId())) {
                throw validacion("VALIDACION", "Pregunta repetida o sin id.");
            }
            mapa.put(r.preguntaId(), r.opcionId());
        }
        for (Pregunta p : caso.preguntas()) {
            if (!mapa.containsKey(p.id())) {
                throw validacion("VALIDACION", "Falta la pregunta " + p.id() + ".");
            }
            String o = mapa.get(p.id());
            if (o != null && !p.opciones().containsKey(o)) {
                throw validacion("VALIDACION", "La opción " + o + " no existe en la pregunta " + p.id() + ".");
            }
        }
        return mapa;
    }

    private void validarMetadatos(Fila s, EnvioResultado e) {
        if (!PROTAGONISTAS.contains(e.protagonista())) {
            throw validacion("VALIDACION", "protagonista debe ser Andrea o Mateo.");
        }
        if (!DESENLACES.contains(e.desenlace())) {
            throw validacion("VALIDACION", "desenlace no reconocido.");
        }
        if (e.inicio() == null || e.fin() == null || !e.fin().isAfter(e.inicio())) {
            throw validacion("VALIDACION", "fin debe ser posterior a inicio.");
        }
        if (e.segundosParticipacion() == null || e.segundosParticipacion().signum() < 0) {
            throw validacion("VALIDACION", "segundosParticipacion no es válido.");
        }
        long real = Duration.between(s.canjeadaEn(), LocalDateTime.now(reloj)).toSeconds() + propiedades.toleranciaSegundos();
        if (e.segundosParticipacion().compareTo(BigDecimal.valueOf(real)) > 0) {
            throw validacion("VALIDACION", "segundosParticipacion supera el tiempo transcurrido desde el canje.");
        }
    }

    private Optional<ResultadoRecibido> resultadoDe(String envioId, String sesionId) {
        List<Map<String, Object>> f = jdbc.queryForList("SELECT sesion_id, puntaje, nota, nivel, difiere FROM ecos_resultado WHERE envio_id = ?", envioId);
        if (f.isEmpty()) {
            return Optional.empty();
        }
        if (!sesionId.equals(f.get(0).get("sesion_id"))) {
            throw new EcosException(HttpStatus.CONFLICT, "ENVIO_DE_OTRA_SESION", "Ese envioId ya pertenece a otra sesión.");
        }
        List<String> fortalezas = jdbc.queryForList("SELECT criterio_id FROM ecos_resultado_criterio WHERE envio_id = ? AND tipo = 'FORTALEZA' ORDER BY criterio_id", String.class, envioId);
        List<String> mejoras = jdbc.queryForList("SELECT criterio_id FROM ecos_resultado_criterio WHERE envio_id = ? AND tipo = 'MEJORA' ORDER BY criterio_id", String.class, envioId);
        Map<String, Object> r = f.get(0);
        return Optional.of(new ResultadoRecibido(ModeloEcos.CONTRATO, envioId, "RECIBIDO", (BigDecimal) r.get("puntaje"),
                (BigDecimal) r.get("nota"), (String) r.get("nivel"), Boolean.TRUE.equals(r.get("difiere")), fortalezas, mejoras));
    }

    /** El token debe ser de esta sesión y del mismo estudiante que la creó. */
    private Fila autenticar(String sesionId, String encabezado) {
        TokenEcos.Reclamos r = tokens.verificar(encabezado);
        List<Fila> f = jdbc.query("SELECT sesion_id, estudiante_id, caso_version, intento, estado, canjeada_en, vence_en FROM ecos_sesion WHERE sesion_id = ?",
                this::fila, sesionId);
        if (f.isEmpty() || !sesionId.equals(r.sesionId()) || !f.get(0).estudianteId().equals(r.estudianteId())) {
            throw new EcosException(HttpStatus.UNAUTHORIZED, "TOKEN_INVALIDO", "El token no pertenece a esta sesión.");
        }
        return f.get(0);
    }

    private Fila fila(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        Timestamp canje = rs.getTimestamp(6);
        return new Fila(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                canje == null ? null : canje.toLocalDateTime(), rs.getTimestamp(7).toLocalDateTime());
    }

    /** "Laura Gómez Pérez" → "Laura G.": solo se muestra en el informe, nunca se guarda en el juego. */
    static String nombreVisible(String completo) {
        if (completo == null || completo.isBlank()) {
            return "Estudiante";
        }
        String[] partes = completo.trim().split("\\s+");
        return partes.length == 1 ? partes[0] : partes[0] + " " + Character.toUpperCase(partes[1].charAt(0)) + ".";
    }

    private Timestamp ahora() {
        return Timestamp.valueOf(LocalDateTime.now(reloj));
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    private static String uuid(String valor) {
        try {
            return UUID.fromString(valor).toString();
        } catch (IllegalArgumentException ex) {
            throw validacion("VALIDACION", "envioId debe ser un UUID.");
        }
    }

    private static EcosException validacion(String codigo, String mensaje) {
        return new EcosException(HttpStatus.UNPROCESSABLE_ENTITY, codigo, mensaje);
    }
}
