package co.edu.humboldt.plataformacasos.casos.repositorio;

import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;

import java.time.LocalDateTime;

/** Proyección para los listados: evita cargar el agregado completo por cada caso. */
public record CasoResumen(Long id, String nombre, Integer tiempoEstimadoMinutos, EstadoCaso estado,
                          LocalDateTime fechaActualizacion, LocalDateTime fechaPublicacion) {
}
