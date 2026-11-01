package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
/*
 #REQUISITO RF-04 || RF-18
 Patrón: BUILDER
 Para qué sirve: representa una sesión (práctica) que el docente quiere programar:
 caso, espacio académico, fechas, tiempo máximo, lugar, mensaje y estudiantes.
 Tiene 10 campos (varios opcionales); el Builder evita un constructor gigante
 y deja el objeto inmutable una vez construido.
 */
public final class SesionProgramada {

    private final Long idCaso;
    private final Long idDocente;
    private final String espacioAcademico;          // RF-04
    private final LocalDateTime fechaInicio;        // RF-18
    private final LocalDateTime fechaFin;           // RF-18
    private final Integer tiempoMaximoMinutos;      // RF-18
    private final String lugar;                     // RF-18 (opcional: solo presencial)
    private final String mensajePersonalizado;      // RF-18 (opcional)
    private final String urlAcceso;                 // opcional
    private final List<Long> idsEstudiantes;        // RF-04: uno o varios

    private SesionProgramada(Builder b) {
        this.idCaso = b.idCaso;
        this.idDocente = b.idDocente;
        this.espacioAcademico = b.espacioAcademico;
        this.fechaInicio = b.fechaInicio;
        this.fechaFin = b.fechaFin;
        this.tiempoMaximoMinutos = b.tiempoMaximoMinutos;
        this.lugar = b.lugar;
        this.mensajePersonalizado = b.mensajePersonalizado;
        this.urlAcceso = b.urlAcceso;
        this.idsEstudiantes = List.copyOf(b.idsEstudiantes);
    }

    public Long idCaso() { return idCaso; }
    public Long idDocente() { return idDocente; }
    public String espacioAcademico() { return espacioAcademico; }
    public LocalDateTime fechaInicio() { return fechaInicio; }
    public LocalDateTime fechaFin() { return fechaFin; }
    public Integer tiempoMaximoMinutos() { return tiempoMaximoMinutos; }
    public String lugar() { return lugar; }
    public String mensajePersonalizado() { return mensajePersonalizado; }
    public String urlAcceso() { return urlAcceso; }
    public List<Long> idsEstudiantes() { return idsEstudiantes; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long idCaso;
        private Long idDocente;
        private String espacioAcademico;
        private LocalDateTime fechaInicio;
        private LocalDateTime fechaFin;
        private Integer tiempoMaximoMinutos;
        private String lugar;
        private String mensajePersonalizado;
        private String urlAcceso;
        private List<Long> idsEstudiantes = List.of();

        public Builder idCaso(Long v) { this.idCaso = v; return this; }
        public Builder idDocente(Long v) { this.idDocente = v; return this; }
        public Builder espacioAcademico(String v) { this.espacioAcademico = v; return this; }
        public Builder fechaInicio(LocalDateTime v) { this.fechaInicio = v; return this; }
        public Builder fechaFin(LocalDateTime v) { this.fechaFin = v; return this; }
        public Builder tiempoMaximoMinutos(Integer v) { this.tiempoMaximoMinutos = v; return this; }
        public Builder lugar(String v) { this.lugar = v; return this; }
        public Builder mensajePersonalizado(String v) { this.mensajePersonalizado = v; return this; }
        public Builder urlAcceso(String v) { this.urlAcceso = v; return this; }
        public Builder idsEstudiantes(List<Long> v) {
            this.idsEstudiantes = (v == null) ? List.of() : v;
            return this;
        }

        // Solo exige lo imprescindible; las reglas de negocio las revisa la cadena de validación.
        public SesionProgramada build() {
            Objects.requireNonNull(idCaso, "idCaso es obligatorio");
            Objects.requireNonNull(idDocente, "idDocente es obligatorio");
            Objects.requireNonNull(fechaInicio, "fechaInicio es obligatoria");
            Objects.requireNonNull(fechaFin, "fechaFin es obligatoria");
            Objects.requireNonNull(tiempoMaximoMinutos, "tiempoMaximoMinutos es obligatorio");
            return new SesionProgramada(this);
        }
    }
}
