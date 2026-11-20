package co.edu.humboldt.plataformacasos.casos.repositorio;

import java.time.LocalDateTime;

/** Caso en la papelera. */
public record CasoEliminado(Long id, String nombre, String estado, LocalDateTime fechaEliminacion) {
}
