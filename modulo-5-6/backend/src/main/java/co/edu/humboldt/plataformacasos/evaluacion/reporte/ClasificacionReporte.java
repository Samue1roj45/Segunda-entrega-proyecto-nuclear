package co.edu.humboldt.plataformacasos.evaluacion.reporte;

import co.edu.humboldt.plataformacasos.evaluacion.participacion.EstadoParticipacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;
import co.edu.humboldt.plataformacasos.evaluacion.resultado.Resultado;

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
