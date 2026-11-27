package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl.*;
import co.edu.humboldt.plataformacasos.casos.servicio.GestionCasos;
import co.edu.humboldt.plataformacasos.casos.servicio.PublicacionCasos;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RangoDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.ResultadoImportacionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte el catálogo de «Ecos de una decisión» en un caso normal del editor (BORRADOR del docente): sus actos son
 * los escenarios, cada pregunta con sus opciones y la rúbrica con los criterios. Desde ahí se edita, publica y programa
 * como cualquier otro caso.
 */
@Service
public class CasoDesdeEcos {

    public static final String NOMBRE = "Ecos de una decisión";
    private static final Map<String, String> CONTEXTOS = Map.of(
            "Actos1-2", "Llegas con la brigada de salud al barrio La Esperanza. Debes reunir pistas del contexto y hablar con Daniela, de 22 años, que vive con Julián y con su hija Luna, de 3.",
            "Acto3", "Esa noche Daniela llega al Hospital San Rafael con heridas de arma cortopunzante y Luna sin signos vitales. Acompañas a la familia (Rosalba, Kevin y Yuliana), das la noticia con el protocolo EPICEE y atiendes a Daniela en la habitación 214.",
            "Actos4-5", "Quince días después, Daniela decide ir a la Comisaría de Familia. Participas en la valoración psicosocial, entrevistas a Daniela, hablas con Rosalba y Yuliana, y la comisaria Vargas decide las medidas de protección.");
    private static final Map<String, String> TITULOS = Map.of(
            "Actos1-2", "Actos 1 y 2: el barrio y la brigada", "Acto3", "Acto 3: el hospital (Escenario 1)",
            "Actos4-5", "Actos 4 y 5: la Comisaría (Escenario 2)");
    private static final String CODIGOS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final JdbcTemplate jdbc;
    private final GestionCasos casos;
    private final PublicacionCasos publicacion;

    public CasoDesdeEcos(JdbcTemplate jdbc, GestionCasos casos, PublicacionCasos publicacion) {
        this.jdbc = jdbc;
        this.casos = casos;
        this.publicacion = publicacion;
    }

    @Transactional
    public ResultadoImportacionDTO crear(Long docenteId) {
        Integer existentes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM casos_estudio WHERE id_docente = ? AND nombre = ? AND fecha_eliminacion IS NULL", Integer.class, docenteId, NOMBRE);
        if (existentes != null && existentes > 0) {
            throw new EcosException(HttpStatus.CONFLICT, "CASO_YA_EXISTE", "Ya tienes el caso «" + NOMBRE + "» en tus casos.");
        }
        List<String> version = jdbc.queryForList("SELECT caso_version FROM ecos_caso ORDER BY cargado_en DESC, caso_version LIMIT 1", String.class);
        if (version.isEmpty()) {
            throw new EcosException(HttpStatus.CONFLICT, "SIN_CATALOGO", "Primero el administrador debe cargar el catálogo de Ecos de una decisión.");
        }
        ResultadoImportacionDTO r = casos.importarDsl(documento(version.get(0)));
        publicacion.publicar(r.caso().id());
        return r;
    }

    DocumentoDsl documento(String version) {
        Map<String, List<Object[]>> opciones = new LinkedHashMap<>();
        jdbc.query("SELECT pregunta_id, texto, grado, credito, retroalimentacion FROM ecos_opcion WHERE caso_version = ? ORDER BY pregunta_id, opcion_id",
                rs -> { opciones.computeIfAbsent(rs.getString(1), k -> new ArrayList<>())
                        .add(new Object[] {rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getString(5)}); }, version);

        Map<String, List<PreguntaDsl>> porActo = new LinkedHashMap<>();
        jdbc.query("SELECT pregunta_id, escenario, enunciado FROM ecos_pregunta WHERE caso_version = ? ORDER BY orden", rs -> {
            porActo.computeIfAbsent(rs.getString(2), k -> new ArrayList<>())
                    .add(new PreguntaDsl(rs.getString(3), respuestas(opciones.getOrDefault(rs.getString(1), List.of()))));
        }, version);

        List<EscenarioDsl> escenarios = new ArrayList<>();
        int n = 1;
        for (Map.Entry<String, List<PreguntaDsl>> acto : porActo.entrySet()) {
            String nombre = TITULOS.getOrDefault(acto.getKey(), "Acto " + n + " (" + acto.getKey() + ")");
            escenarios.add(new EscenarioDsl(recortar(nombre, 150), CONTEXTOS.getOrDefault(acto.getKey(),
                    "Decisiones del videojuego «" + NOMBRE + "». Elige la opción que consideres más adecuada."), acto.getValue()));
            n++;
        }

        List<CriterioDsl> criterios = jdbc.query("SELECT nombre FROM ecos_criterio WHERE caso_version = ? ORDER BY criterio_id",
                (rs, i) -> new CriterioDsl(recortar(rs.getString(1), 150), List.of(rs.getString(1)), new RangoDTO(BigDecimal.ZERO, new BigDecimal("100"))), version);
        RubricaDsl rubrica = new RubricaDsl("Criterios de «" + NOMBRE + "»", "Criterios que SOMA evalúa con las decisiones del estudiante.", criterios);

        return new DocumentoDsl(DocumentoDsl.NOMBRE, DocumentoDsl.VERSION, new CasoDsl(NOMBRE,
                "Caso 1. Violencia familiar y tentativa de feminicidio. Daniela, de 22 años y madre de Luna (3 años), vive en un barrio vulnerable con Julián (28), que la maltrata psicológicamente. Una noche él la ataca con un arma cortopunzante: Luna muere y Daniela sobrevive con 28 heridas. Es una simulación formativa de psicología; no diagnostica ni sustituye la atención profesional.",
                "Videojuego en Unreal Engine de seis actos. Eliges protagonista (Andrea o Mateo, psicólogos del Hospital San Rafael); la historia es la misma con cualquiera de los dos. La nota va de 0 a 5,0 y la calcula SOMA con tus decisiones (75 puntos de preguntas del caso y 25 de decisiones narrativas).", 50, escenarios, rubrica, List.of()));
    }

    /** Una sola correcta (la de mayor crédito); el resto se evalúa por su porcentaje de crédito. */
    private static List<RespuestaDsl> respuestas(List<Object[]> filas) {
        int correcta = 0;
        BigDecimal mejor = new BigDecimal("-1");
        for (int i = 0; i < filas.size(); i++) {
            BigDecimal c = (BigDecimal) filas.get(i)[2];
            if (c.compareTo(mejor) > 0) { mejor = c; correcta = i; }
        }
        List<RespuestaDsl> r = new ArrayList<>();
        for (int i = 0; i < filas.size() && i < CODIGOS.length(); i++) {
            Object[] f = filas.get(i);
            String retro = f[3] == null || ((String) f[3]).isBlank() ? null : (String) f[3];
            String justificacion = retro != null ? retro : switch ((String) f[1]) {
                case "Correct" -> "Es la decisión más adecuada en esta situación.";
                case "Partial" -> "Es una decisión parcialmente acertada: suma una parte de los puntos.";
                default -> "No es la decisión más adecuada en esta situación.";
            };
            BigDecimal porcentaje = ((BigDecimal) f[2]).multiply(new BigDecimal("100")).setScale(2, java.math.RoundingMode.HALF_UP);
            r.add(new RespuestaDsl(String.valueOf(CODIGOS.charAt(i)), (String) f[0], i == correcta, porcentaje, justificacion));
        }
        return r;
    }

    private static String recortar(String t, int max) { return t.length() <= max ? t : t.substring(0, max); }
}
