package co.edu.humboldt.plataformacasos.integracion;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.PoliticaInicioIntento;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioIngresoTardio;
import org.springframework.stereotype.Component;

/**
 * Módulo 4 → módulo 5: la regla de ingreso tardío (RF-22, Strategy PoliticaIngresoTardio) es una sola;
 * el módulo 5 la aplica antes de registrar el inicio de un intento nuevo (RF-12).
 */
@Component
public class PoliticaInicioDesdeSesiones implements PoliticaInicioIntento {

    private final ServicioIngresoTardio ingreso;

    public PoliticaInicioDesdeSesiones(ServicioIngresoTardio ingreso) {
        this.ingreso = ingreso;
    }

    @Override
    public void exigirInicioPermitido(Long autorizacionId) {
        ingreso.exigirPermitido(autorizacionId);
    }
}
