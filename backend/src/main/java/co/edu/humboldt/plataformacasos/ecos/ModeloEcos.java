package co.edu.humboldt.plataformacasos.ecos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Tipos de datos del contrato ecos-soma/1 (entradas del juego, salidas de SOMA y catálogo). */
public final class ModeloEcos {

    private ModeloEcos() {
    }

    public static final String CONTRATO = "ecos-soma/1";

    // ---- Catálogo que entrega el juego (banco de preguntas normalizado) ----

    public record CatalogoImportado(String casoVersion, String casoId, String nombre, boolean creditoParcial,
                                    List<CriterioImportado> criterios, List<PreguntaImportada> preguntas) {
    }

    public record CriterioImportado(String id, String nombre) {
    }

    public record PreguntaImportada(String id, String escenario, boolean nucleo, BigDecimal puntos, String enunciado,
                                    List<String> criterios, List<OpcionImportada> opciones) {
    }

    public record OpcionImportada(String id, String grado, BigDecimal credito, String texto, String retroalimentacion) {
    }

    // ---- Catálogo ya cargado, para calcular ----

    public record Opcion(String id, String grado, BigDecimal credito) {
    }

    public record Pregunta(String id, BigDecimal puntos, List<String> criterios, Map<String, Opcion> opciones) {
    }

    public record CasoCatalogo(String version, String casoId, String nombre, List<Pregunta> preguntas, List<String> criterios) {
    }

    // ---- Entradas del juego ----

    public record CasoRef(String id, String version) {
    }

    public record RespuestaJuego(String preguntaId, String opcionId) {
    }

    public record CriterioJuego(String id, BigDecimal obtenido, BigDecimal posible) {
    }

    public record CalculoJuego(BigDecimal puntaje, BigDecimal posible, BigDecimal nota, String nivel,
                               Integer aciertosNucleo, Integer totalNucleo, List<CriterioJuego> criterios,
                               List<String> fortalezas, List<String> mejoras) {
    }

    public record DatosJuego(String build, String motor) {
    }

    public record EnvioResultado(String contrato, String envioId, String sesionId, Integer intento, CasoRef caso,
                                 DatosJuego juego, OffsetDateTime inicio, OffsetDateTime fin,
                                 BigDecimal segundosParticipacion, String protagonista,
                                 List<RespuestaJuego> respuestas, String desenlace, Map<String, Integer> indicadores,
                                 CalculoJuego calculoJuego) {
    }

    public record EventoJuego(String envioId, String punto, Integer acto, OffsetDateTime fecha) {
    }

    // ---- Salidas de SOMA ----

    public record EstudianteCanje(String id, String nombreVisible) {
    }

    public record RespuestaCanje(String contrato, String sesionId, String token, EstudianteCanje estudiante,
                                 CasoRef caso, int intento, boolean reanudar) {
    }

    public record SesionCreada(String sesionId, String codigo, String enlace, LocalDateTime venceEn, int intento) {
    }

    public record ResultadoRecibido(String contrato, String envioId, String estado, BigDecimal puntaje, BigDecimal nota,
                                    String nivel, boolean revision, List<String> fortalezas, List<String> mejoras) {
    }

    /** oficial: el mejor intento del estudiante (a igual nota, el primero) es el que cuenta como nota oficial. */
    public record IntentoEcos(String sesionId, int intento, String estado, LocalDateTime creada, BigDecimal puntaje,
                              BigDecimal nota, String nivel, String desenlace, LocalDateTime fin, boolean oficial) {
    }

    public record EstadoEstudiante(String caso, boolean disponible, int maxIntentos, int usados, int restantes,
                                   List<IntentoEcos> intentos) {
    }

    /** Fila de la vista del docente: un resultado de Ecos de uno de sus estudiantes. */
    public record ResultadoDocente(String sesionId, String estudiante, int intento, BigDecimal puntaje, BigDecimal nota,
                                   String nivel, String desenlace, String protagonista, BigDecimal segundos, boolean revision,
                                   LocalDateTime fin, List<String> fortalezas, List<String> mejoras, boolean oficial) {
    }

    /** Una fila del tablero en vivo del docente: quién está jugando y en qué punto va. */
    public record EnVivo(String sesionId, String estudiante, String estado, int intento, String ultimoPunto, Integer ultimoActo,
                         LocalDateTime canjeadaEn, LocalDateTime ultimoEventoEn, LocalDateTime venceEn, BigDecimal nota, String nivel) {
    }

    /** Un estudiante del docente con su avance en Ecos. estadoActual: SIN_JUGAR, CODIGO_GENERADO, JUGANDO o TERMINADO. */
    public record EstudianteDocente(long id, String nombre, String correo, LocalDateTime vinculadoEn, int intentosUsados,
                                    int intentosMaximos, int repeticiones, BigDecimal mejorNota, String nivel, String estadoActual) {
    }
}
