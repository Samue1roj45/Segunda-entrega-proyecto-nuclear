package com.unihumboldt.nuclear.evaluacion.participacion;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;

public final class Rn17 {

    private Rn17() {
    }

    public static OffsetDateTime puntoMedio(PracticaRef practica) {
        Duration duracion = Duration.between(practica.fechaInicio(), practica.fechaFin());
        return practica.fechaInicio().plus(duracion.dividedBy(2));
    }

    public static void validarVentanaInicio(PracticaRef practica, OffsetDateTime ahora) {
        if (ahora.isAfter(puntoMedio(practica))) {
            throw new ReglaNegocioException(
                    "No puedes iniciar la participación: ya transcurrió la mitad del tiempo "
                            + "establecido para la práctica.");
        }
    }
}
