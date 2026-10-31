package com.unihumboldt.nuclear.evaluacion.reporte;

import com.unihumboldt.nuclear.evaluacion.participacion.EstadoParticipacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.resultado.Resultado;

final class ClasificacionReporte {

    private ClasificacionReporte() {
    }

    static String de(Participacion participacion, Resultado resultado) {
        if (participacion.getEstado() == EstadoParticipacion.INCOMPLETA) {
            return "INCOMPLETA";
        }
        if (participacion.getEstado() == EstadoParticipacion.NO_INICIADA && resultado != null) {
            return "AUSENCIA";
        }
        return participacion.getEstado().name();
    }
}
