package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "participacion",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_participacion_autorizacion_intento",
                columnNames = {"autorizacion_id", "numero_intento"}))
public class Participacion {

    public static final int MAX_INTENTOS = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "practica_id", nullable = false)
    private Long practicaId;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "autorizacion_id", nullable = false)
    private Long autorizacionId;

    @Column(name = "numero_intento", nullable = false)
    private int numeroIntento = 1;

    @Column
    private OffsetDateTime inicio;

    @Column
    private OffsetDateTime fin;

    @Column(name = "tiempo_usado_seg", nullable = false)
    private int tiempoUsadoSeg = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoParticipacion estado = EstadoParticipacion.NO_INICIADA;

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

    public Long getAutorizacionId() {
        return autorizacionId;
    }

    public void setAutorizacionId(Long autorizacionId) {
        this.autorizacionId = autorizacionId;
    }

    public int getNumeroIntento() {
        return numeroIntento;
    }

    public void setNumeroIntento(int numeroIntento) {
        this.numeroIntento = numeroIntento;
    }

    public OffsetDateTime getInicio() {
        return inicio;
    }

    public void setInicio(OffsetDateTime inicio) {
        this.inicio = inicio;
    }

    public OffsetDateTime getFin() {
        return fin;
    }

    public void setFin(OffsetDateTime fin) {
        this.fin = fin;
    }

    public int getTiempoUsadoSeg() {
        return tiempoUsadoSeg;
    }

    public void setTiempoUsadoSeg(int tiempoUsadoSeg) {
        this.tiempoUsadoSeg = tiempoUsadoSeg;
    }

    public EstadoParticipacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoParticipacion estado) {
        this.estado = estado;
    }

    public boolean esTerminal() {
        return estado == EstadoParticipacion.FINALIZADA || estado == EstadoParticipacion.INCOMPLETA;
    }
}
