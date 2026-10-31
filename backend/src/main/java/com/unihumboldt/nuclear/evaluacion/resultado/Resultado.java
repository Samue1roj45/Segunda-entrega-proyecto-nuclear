package com.unihumboldt.nuclear.evaluacion.resultado;

import java.math.BigDecimal;
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
@Table(name = "resultado")
public class Resultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "participacion_id", nullable = false, unique = true)
    private Participacion participacion;

    @Column(nullable = false)
    private int correctas;

    @Column(nullable = false)
    private int incorrectas;

    @Column(name = "no_respondidas", nullable = false)
    private int noRespondidas;

    @Column(name = "peso_obtenido", nullable = false)
    private int pesoObtenido;

    @Column(name = "peso_total", nullable = false)
    private int pesoTotal;

    @Column(name = "escala_maxima", nullable = false)
    private int escalaMaxima;

    @Column(name = "nota_final", nullable = false, precision = 6, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "fecha_calculo", nullable = false)
    private OffsetDateTime fechaCalculo;

    public Long getId() {
        return id;
    }

    public Participacion getParticipacion() {
        return participacion;
    }

    public void setParticipacion(Participacion participacion) {
        this.participacion = participacion;
    }

    public int getCorrectas() {
        return correctas;
    }

    public void setCorrectas(int correctas) {
        this.correctas = correctas;
    }

    public int getIncorrectas() {
        return incorrectas;
    }

    public void setIncorrectas(int incorrectas) {
        this.incorrectas = incorrectas;
    }

    public int getNoRespondidas() {
        return noRespondidas;
    }

    public void setNoRespondidas(int noRespondidas) {
        this.noRespondidas = noRespondidas;
    }

    public int getPesoObtenido() {
        return pesoObtenido;
    }

    public void setPesoObtenido(int pesoObtenido) {
        this.pesoObtenido = pesoObtenido;
    }

    public int getPesoTotal() {
        return pesoTotal;
    }

    public void setPesoTotal(int pesoTotal) {
        this.pesoTotal = pesoTotal;
    }

    public int getEscalaMaxima() {
        return escalaMaxima;
    }

    public void setEscalaMaxima(int escalaMaxima) {
        this.escalaMaxima = escalaMaxima;
    }

    public BigDecimal getNotaFinal() {
        return notaFinal;
    }

    public void setNotaFinal(BigDecimal notaFinal) {
        this.notaFinal = notaFinal;
    }

    public OffsetDateTime getFechaCalculo() {
        return fechaCalculo;
    }

    public void setFechaCalculo(OffsetDateTime fechaCalculo) {
        this.fechaCalculo = fechaCalculo;
    }
}
