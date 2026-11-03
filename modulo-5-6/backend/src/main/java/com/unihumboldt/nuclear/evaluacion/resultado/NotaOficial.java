package com.unihumboldt.nuclear.evaluacion.resultado;

import java.time.OffsetDateTime;

import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "nota_oficial",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_nota_oficial_practica_estudiante",
                columnNames = {"practica_id", "estudiante_id"}))
public class NotaOficial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "practica_id", nullable = false)
    private Long practicaId;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "participacion_id", nullable = false)
    private Participacion participacion;

    @Column(name = "docente_id", nullable = false)
    private Long docenteId;

    @Column(name = "fecha_seleccion", nullable = false)
    private OffsetDateTime fechaSeleccion;

    public Long getId() {
        return id;
    }

    public Long getPracticaId() {
        return practicaId;
    }

    public void setPracticaId(Long practicaId) {
        this.practicaId = practicaId;
    }

    public Long getEstudianteId() {
        return estudianteId;
    }

    public void setEstudianteId(Long estudianteId) {
        this.estudianteId = estudianteId;
    }

    public Participacion getParticipacion() {
        return participacion;
    }

    public void setParticipacion(Participacion participacion) {
        this.participacion = participacion;
    }

    public Long getDocenteId() {
        return docenteId;
    }

    public void setDocenteId(Long docenteId) {
        this.docenteId = docenteId;
    }

    public OffsetDateTime getFechaSeleccion() {
        return fechaSeleccion;
    }

    public void setFechaSeleccion(OffsetDateTime fechaSeleccion) {
        this.fechaSeleccion = fechaSeleccion;
    }
}
