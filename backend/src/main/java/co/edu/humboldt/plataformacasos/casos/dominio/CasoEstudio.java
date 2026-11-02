package co.edu.humboldt.plataformacasos.casos.dominio;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Raíz del agregado del caso de estudio (RF-05). Todo cambio de contenido pasa por
 * {@link #prepararCambio()}, de modo que un caso publicado o archivado no se puede alterar
 * aunque un servicio olvide comprobarlo.
 */
@Entity
@Table(name = "casos_estudio")
public class CasoEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caso")
    private Long id;

    @Column(name = "id_docente", nullable = false, updatable = false)
    private Long docenteId;

    @Column(length = LimitesDsl.LONGITUD_NOMBRE)
    private String nombre;

    @Column(name = "desarrollo_situacional", columnDefinition = "TEXT")
    private String desarrolloSituacional;

    @Column(name = "contexto_historia", columnDefinition = "TEXT")
    private String contextoHistoria;

    @Column(name = "tiempo_estimado_min")
    private Integer tiempoEstimadoMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCaso estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_archivado")
    private LocalDateTime fechaArchivado;

    /** Requerido por JPA. */
    protected CasoEstudio() {
    }

    private CasoEstudio(Long docenteId) {
        this.docenteId = Objects.requireNonNull(docenteId, "docenteId");
        this.estado = EstadoCaso.BORRADOR;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    /** RF-09: todo caso nuevo nace en BORRADOR y puede estar incompleto. */
    public static CasoEstudio nuevoBorrador(Long docenteId, String nombre, String desarrolloSituacional,
                                            String contextoHistoria, Integer tiempoEstimadoMinutos) {
        CasoEstudio caso = new CasoEstudio(docenteId);
        caso.asignarDatos(nombre, desarrolloSituacional, contextoHistoria, tiempoEstimadoMinutos);
        return caso;
    }

    public void actualizarDatos(String nombre, String desarrolloSituacional, String contextoHistoria,
                                Integer tiempoEstimadoMinutos) {
        prepararCambio();
        asignarDatos(nombre, desarrolloSituacional, contextoHistoria, tiempoEstimadoMinutos);
    }

    private void asignarDatos(String nombre, String desarrolloSituacional, String contextoHistoria,
                              Integer tiempoEstimadoMinutos) {
        this.nombre = TextoDominio.normalizar(nombre, LimitesDsl.LONGITUD_NOMBRE, "nombre");
        this.desarrolloSituacional = TextoDominio.normalizar(desarrolloSituacional, LimitesDsl.LONGITUD_TEXTO,
                "desarrolloSituacional");
        this.contextoHistoria = TextoDominio.normalizar(contextoHistoria, LimitesDsl.LONGITUD_TEXTO,
                "contextoHistoria");
        this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
    }

    /** Punto único de control: rechaza el cambio si el estado no lo permite y registra la fecha. */
    void prepararCambio() {
        if (!estado.permiteEditarContenido()) {
            throw new ConflictoException("El caso está " + estado + " y su contenido no se puede modificar.");
        }
        fechaActualizacion = LocalDateTime.now();
    }

    public boolean perteneceA(Long idDocente) {
        return docenteId.equals(idDocente);
    }

    public Long getId() {
        return id;
    }

    public Long getDocenteId() {
        return docenteId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDesarrolloSituacional() {
        return desarrolloSituacional;
    }

    public String getContextoHistoria() {
        return contextoHistoria;
    }

    public Integer getTiempoEstimadoMinutos() {
        return tiempoEstimadoMinutos;
    }

    public EstadoCaso getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public LocalDateTime getFechaArchivado() {
        return fechaArchivado;
    }
}
