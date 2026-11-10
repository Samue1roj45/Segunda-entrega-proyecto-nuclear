package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.validacion.Incumplimiento;

import java.time.LocalDateTime;
import java.util.List;

/** Mismos campos que ErrorApi del Módulo 1, más las reglas que el docente debe corregir. */
public record ErrorPublicacionApi(LocalDateTime fecha, int estado, String error, String mensaje, String ruta,
                                  List<Incumplimiento> incumplimientos) {
}
