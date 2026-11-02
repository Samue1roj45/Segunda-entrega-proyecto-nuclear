package co.edu.humboldt.plataformacasos.casos.validacion;

import java.util.List;

/** Resultado de evaluar las reglas del DSL-CEPS sobre un caso (RF-09). */
public record InformeValidacion(boolean valido, List<Incumplimiento> incumplimientos) {

    public static InformeValidacion de(List<Incumplimiento> incumplimientos) {
        return new InformeValidacion(incumplimientos.isEmpty(), List.copyOf(incumplimientos));
    }
}
