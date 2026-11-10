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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Escenario académico de un caso (RF-06): nombre, contexto situacional y preguntas. */
@Entity
@Table(name = "escenarios")
public class Escenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_escenario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caso", nullable = false, updatable = false)
    private CasoEstudio caso;

    @Column(length = LimitesDsl.LONGITUD_NOMBRE)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String contexto;

    @Column(nullable = false)
    private int orden;

    @OneToMany(mappedBy = "escenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    @BatchSize(size = LimitesDsl.TAMANO_LOTE_CONSULTA)
    private List<Pregunta> preguntas = new ArrayList<>();

    /** Requerido por JPA. */
    protected Escenario() {
    }

    Escenario(CasoEstudio caso, int orden, String nombre, String contexto) {
        this.caso = caso;
        this.orden = orden;
        asignarDatos(nombre, contexto);
    }

    public void actualizar(String nombre, String contexto) {
        prepararCambio();
        asignarDatos(nombre, contexto);
    }

    private void asignarDatos(String nombre, String contexto) {
        this.nombre = TextoDominio.normalizar(nombre, LimitesDsl.LONGITUD_NOMBRE, "nombre");
        this.contexto = TextoDominio.normalizar(contexto, LimitesDsl.LONGITUD_TEXTO, "contexto");
    }

    public Pregunta agregarPregunta(String enunciado) {
        prepararCambio();
        int siguienteOrden = preguntas.stream().mapToInt(Pregunta::getOrden).max().orElse(0) + 1;
        Pregunta pregunta = new Pregunta(this, siguienteOrden, enunciado);
        preguntas.add(pregunta);
        return pregunta;
    }

    public void eliminarPregunta(Long idPregunta) {
        prepararCambio();
        Pregunta pregunta = buscarPregunta(idPregunta)
                .orElseThrow(() -> new RecursoNoEncontradoException("La pregunta no existe en este caso."));
        preguntas.remove(pregunta);
    }

    Optional<Pregunta> buscarPregunta(Long idPregunta) {
        return preguntas.stream().filter(p -> idPregunta.equals(p.getId())).findFirst();
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

    public String getContexto() {
        return contexto;
    }

    public int getOrden() {
        return orden;
    }

    public List<Pregunta> getPreguntas() {
        return Collections.unmodifiableList(preguntas);
    }
}
