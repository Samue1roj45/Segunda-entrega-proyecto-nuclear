package com.unihumboldt.nuclear.evaluacion.notificacion;

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
        name = "notificacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_notificacion_clave", columnNames = "clave_idempotencia"))
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacion tipo;

    @Column(name = "clave_idempotencia", nullable = false, length = 180)
    private String claveIdempotencia;

    @Column(name = "practica_id")
    private Long practicaId;

    @Column(name = "estudiante_id", nullable = false)
    private Long estudianteId;

    @Column(name = "participacion_id")
    private Long participacionId;

    @Column(name = "destinatario_correo", length = 200)
    private String destinatarioCorreo;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(nullable = false, length = 4000)
    private String cuerpo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE;

    @Column(name = "error_mensaje", length = 500)
    private String errorMensaje;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    public Long getId() {
        return id;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
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

    public Long getParticipacionId() {
        return participacionId;
    }

    public void setParticipacionId(Long participacionId) {
        this.participacionId = participacionId;
    }

    public String getDestinatarioCorreo() {
        return destinatarioCorreo;
    }

    public void setDestinatarioCorreo(String destinatarioCorreo) {
        this.destinatarioCorreo = destinatarioCorreo;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public EstadoEnvio getEstado() {
        return estado;
    }

    public void setEstado(EstadoEnvio estado) {
        this.estado = estado;
    }

    public String getErrorMensaje() {
        return errorMensaje;
    }

    public void setErrorMensaje(String errorMensaje) {
        this.errorMensaje = errorMensaje;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public OffsetDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(OffsetDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
