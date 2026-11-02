package co.edu.humboldt.plataformacasos.casos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
}
