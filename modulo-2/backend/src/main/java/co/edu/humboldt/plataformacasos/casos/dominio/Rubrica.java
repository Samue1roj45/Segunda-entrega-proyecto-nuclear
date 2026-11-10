package co.edu.humboldt.plataformacasos.casos.dominio;

import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Rúbrica asociada a un caso (RF-17). Solo guarda datos: el cálculo de la nota es del Integrante 4. */
@Entity
@Table(name = "rubricas")
public class Rubrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rubrica")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caso", nullable = false, updatable = false, unique = true)
    private CasoEstudio caso;

    @Column(length = LimitesDsl.LONGITUD_NOMBRE)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @OneToMany(mappedBy = "rubrica", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = LimitesDsl.TAMANO_LOTE_CONSULTA)
    private List<CriterioEvaluacion> criterios = new ArrayList<>();

    /** Requerido por JPA. */
    protected Rubrica() {
    }

    Rubrica(CasoEstudio caso, String nombre, String descripcion) {
        this.caso = caso;
        asignarDatos(nombre, descripcion);
    }

    void asignarDatos(String nombre, String descripcion) {
        this.nombre = TextoDominio.normalizar(nombre, LimitesDsl.LONGITUD_NOMBRE, "nombre");
        this.descripcion = TextoDominio.normalizar(descripcion, LimitesDsl.LONGITUD_TEXTO, "descripcion");
    }

    public CriterioEvaluacion agregarCriterio(String criterio, List<String> parametros, BigDecimal min, BigDecimal max) {
        prepararCambio();
        CriterioEvaluacion nuevo = new CriterioEvaluacion(this, criterio, parametros, min, max);
        criterios.add(nuevo);
        return nuevo;
    }

    public void eliminarCriterio(Long idCriterio) {
        prepararCambio();
        criterios.remove(criterio(idCriterio));
    }

    public CriterioEvaluacion criterio(Long idCriterio) {
        return criterios.stream().filter(c -> idCriterio.equals(c.getId())).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El criterio no existe en esta rúbrica."));
    }

    void prepararCambio() {
        caso.prepararCambio();
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public List<CriterioEvaluacion> getCriterios() {
        return Collections.unmodifiableList(criterios);
    }
}
