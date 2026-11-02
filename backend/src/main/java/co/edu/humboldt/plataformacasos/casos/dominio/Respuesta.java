package co.edu.humboldt.plataformacasos.casos.dominio;

import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Opción de respuesta (RF-07). {@code codigo} es el identificador del DSL (A, B, C...);
 * {@code id} es solo la clave de la base de datos.
 */
@Entity
@Table(name = "respuestas")
public class Respuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pregunta", nullable = false, updatable = false)
    private Pregunta pregunta;

    @Column(length = LimitesDsl.LONGITUD_CODIGO_RESPUESTA)
    private String codigo;

    @Column(columnDefinition = "TEXT")
    private String texto;

    /** Nulo mientras el docente no declare si es correcta o incorrecta (OCL-06). */
    @Column(name = "es_correcta")
    private Boolean esCorrecta;

    @Column(precision = 5, scale = 2)
    private BigDecimal porcentaje;

    /** RF-08: explica por qué la opción es correcta o incorrecta. El estudiante no la ve durante el intento. */
    @Column(columnDefinition = "TEXT")
    private String justificacion;

    /** Requerido por JPA. */
    protected Respuesta() {
    }

    Respuesta(Pregunta pregunta, String codigo, String texto, Boolean esCorrecta, BigDecimal porcentaje,
              String justificacion) {
        this.pregunta = pregunta;
        asignarDatos(codigo, texto, esCorrecta, porcentaje, justificacion);
    }

    public void actualizar(String codigo, String texto, Boolean esCorrecta, BigDecimal porcentaje,
                           String justificacion) {
        prepararCambio();
        asignarDatos(codigo, texto, esCorrecta, porcentaje, justificacion);
    }

    /** RF-08: edita solo la justificación. */
    public void justificar(String justificacion) {
        prepararCambio();
        asignarJustificacion(justificacion);
    }

    private void asignarDatos(String codigo, String texto, Boolean esCorrecta, BigDecimal porcentaje,
                              String justificacion) {
        String codigoLimpio = TextoDominio.normalizar(codigo, LimitesDsl.LONGITUD_CODIGO_RESPUESTA, "codigo");
        this.codigo = codigoLimpio == null ? null : codigoLimpio.toUpperCase(Locale.ROOT);
        this.texto = TextoDominio.normalizar(texto, LimitesDsl.LONGITUD_TEXTO, "texto");
        this.esCorrecta = esCorrecta;
        asignarPorcentaje(porcentaje);
        asignarJustificacion(justificacion);
    }

    private void asignarJustificacion(String justificacion) {
        this.justificacion = TextoDominio.normalizar(justificacion, LimitesDsl.LONGITUD_TEXTO, "justificacion");
    }

    private void asignarPorcentaje(BigDecimal valor) {
        DecimalDominio.exigirPrecision(valor, "porcentaje");
        if (valor != null && (valor.compareTo(LimitesDsl.PORCENTAJE_MINIMO) < 0
                || valor.compareTo(LimitesDsl.PORCENTAJE_MAXIMO) > 0)) {
            throw new SolicitudInvalidaException("El porcentaje debe estar entre "
                    + LimitesDsl.PORCENTAJE_MINIMO + " y " + LimitesDsl.PORCENTAJE_MAXIMO + ".");
        }
        this.porcentaje = valor;
    }

    void declararCorrecta(boolean correcta) {
        this.esCorrecta = correcta;
    }

    void prepararCambio() {
        pregunta.prepararCambio();
    }

    public Long getId() {
        return id;
    }

    public Pregunta getPregunta() {
        return pregunta;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTexto() {
        return texto;
    }

    public Boolean getEsCorrecta() {
        return esCorrecta;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public String getJustificacion() {
        return justificacion;
    }
}
