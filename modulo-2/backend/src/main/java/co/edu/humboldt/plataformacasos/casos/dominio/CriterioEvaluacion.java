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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Criterio de una rúbrica (RF-17): nombre, parámetros o descriptores y rango de evaluación. */
@Entity
@Table(name = "criterios_evaluacion")
public class CriterioEvaluacion {

    private static final String SEPARADOR_PARAMETROS = "\n";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_criterio")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_rubrica", nullable = false, updatable = false)
    private Rubrica rubrica;

    @Column(length = LimitesDsl.LONGITUD_CRITERIO)
    private String criterio;

    /** Una línea por parámetro: el modelo de datos los guarda en una sola columna TEXT. */
    @Column(columnDefinition = "TEXT")
    private String parametros;

    @Column(name = "rango_min", precision = 5, scale = 2)
    private BigDecimal rangoMin;

    @Column(name = "rango_max", precision = 5, scale = 2)
    private BigDecimal rangoMax;

    /** Requerido por JPA. */
    protected CriterioEvaluacion() {
    }

    CriterioEvaluacion(Rubrica rubrica, String criterio, List<String> parametros, BigDecimal min, BigDecimal max) {
        this.rubrica = rubrica;
        asignarDatos(criterio, parametros, min, max);
    }

    public void actualizar(String criterio, List<String> parametros, BigDecimal min, BigDecimal max) {
        rubrica.prepararCambio();
        asignarDatos(criterio, parametros, min, max);
    }

    private void asignarDatos(String criterio, List<String> parametros, BigDecimal min, BigDecimal max) {
        this.criterio = TextoDominio.normalizar(criterio, LimitesDsl.LONGITUD_CRITERIO, "criterio");
        this.parametros = unirParametros(parametros);
        asignarRango(min, max);
    }

    private void asignarRango(BigDecimal min, BigDecimal max) {
        DecimalDominio.exigirPrecision(min, "rango.min");
        DecimalDominio.exigirPrecision(max, "rango.max");
        if (min != null && max != null && min.compareTo(max) >= 0) {
            throw new SolicitudInvalidaException("El rango mínimo debe ser menor que el máximo.");
        }
        this.rangoMin = min;
        this.rangoMax = max;
    }

    private static String unirParametros(List<String> parametros) {
        if (parametros == null) {
            return null;
        }
        String unidos = parametros.stream().filter(Objects::nonNull).map(String::trim)
                .filter(p -> !p.isEmpty()).collect(Collectors.joining(SEPARADOR_PARAMETROS));
        return TextoDominio.normalizar(unidos, LimitesDsl.LONGITUD_TEXTO, "parametros");
    }

    public List<String> getParametros() {
        return parametros == null ? List.of() : Stream.of(parametros.split(SEPARADOR_PARAMETROS)).toList();
    }

    public Long getId() {
        return id;
    }

    public String getCriterio() {
        return criterio;
    }

    public BigDecimal getRangoMin() {
        return rangoMin;
    }

    public BigDecimal getRangoMax() {
        return rangoMax;
    }
}
