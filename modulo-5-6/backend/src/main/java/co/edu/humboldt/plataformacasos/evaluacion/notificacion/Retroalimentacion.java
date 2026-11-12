package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.time.OffsetDateTime;

import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "retroalimentacion")
public class Retroalimentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "participacion_id", nullable = false, unique = true)
    private Participacion participacion;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "practica_id", nullable = false)
    private Long practicaId;

    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    @Column(nullable = false, length = 4000)
    private String texto;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    public Long getId() {
        return id;
    }

    public Participacion getParticipacion() {
        return participacion;
    }

    public void setParticipacion(Participacion participacion) {
        this.participacion = participacion;
    }

    public Long getEstudianteId() {
        return estudianteId;
    }

    public void setEstudianteId(Long estudianteId) {
        this.estudianteId = estudianteId;
    }

    public Long getPracticaId() {
        return practicaId;
    }

    public void setPracticaId(Long practicaId) {
        this.practicaId = practicaId;
    }

    public Long getDocenteId() {
        return docenteId;
    }

    public void setDocenteId(Long docenteId) {
        this.docenteId = docenteId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(OffsetDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
