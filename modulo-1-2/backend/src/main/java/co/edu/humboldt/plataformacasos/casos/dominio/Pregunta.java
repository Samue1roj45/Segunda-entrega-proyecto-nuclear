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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Pregunta de un escenario. Puede prepararse gradualmente mientras el caso sea un borrador. */
@Entity
@Table(name = "preguntas")
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pregunta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_escenario", nullable = false, updatable = false)
    private Escenario escenario;

    @Column(columnDefinition = "TEXT")
    private String enunciado;

    @Column(nullable = false)
    private int orden;

    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = LimitesDsl.TAMANO_LOTE_CONSULTA)
    private List<Respuesta> respuestas = new ArrayList<>();

    /** Requerido por JPA. */
    protected Pregunta() {
    }

    Pregunta(Escenario escenario, int orden, String enunciado) {
        this.escenario = escenario;
        this.orden = orden;
        asignarEnunciado(enunciado);
    }

    public void actualizarEnunciado(String enunciado) {
        prepararCambio();
        asignarEnunciado(enunciado);
    }

    private void asignarEnunciado(String enunciado) {
        this.enunciado = TextoDominio.normalizar(enunciado, LimitesDsl.LONGITUD_TEXTO, "enunciado");
    }

    public Respuesta agregarRespuesta(String codigo, String texto, Boolean esCorrecta, BigDecimal porcentaje,
                                      String justificacion) {
        prepararCambio();
        Respuesta respuesta = new Respuesta(this, codigo, texto, esCorrecta, porcentaje, justificacion);
        respuestas.add(respuesta);
        return respuesta;
    }

    public void eliminarRespuesta(Long idRespuesta) {
        prepararCambio();
        respuestas.remove(respuestaExistente(idRespuesta));
    }

    /** RF-07: deja la opción indicada como la correcta y las demás como incorrectas, en un solo paso. */
    public void marcarCorrecta(Long idRespuesta) {
        prepararCambio();
        Respuesta correcta = respuestaExistente(idRespuesta);
        respuestas.forEach(r -> r.declararCorrecta(r == correcta));
    }

    Optional<Respuesta> buscarRespuesta(Long idRespuesta) {
        return respuestas.stream().filter(r -> idRespuesta.equals(r.getId())).findFirst();
    }

    private Respuesta respuestaExistente(Long idRespuesta) {
        return buscarRespuesta(idRespuesta)
                .orElseThrow(() -> new RecursoNoEncontradoException("La respuesta no existe en esta pregunta."));
    }

    void prepararCambio() {
        escenario.prepararCambio();
    }

    public Long getId() {
        return id;
    }

    public Escenario getEscenario() {
        return escenario;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public int getOrden() {
        return orden;
    }

    public List<Respuesta> getRespuestas() {
        return Collections.unmodifiableList(respuestas);
    }
}
