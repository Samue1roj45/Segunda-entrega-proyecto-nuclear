package co.edu.humboldt.plataformacasos.sesiones.estado;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import java.util.List;
/*
 #REQUISITO RF-19
 Patrón: STATE (estado concreto). FINALIZADO -> estado terminal, no admite acciones.
 */
public final class SesionFinalizada implements EstadoSesion {

    @Override public String nombre() { return "FINALIZADO"; }

    @Override
    public void iniciar(AccionesSesion acciones, Long idPractica, Long idDocente) {
        throw new ReglaNegocioException("La sesión ya finalizó; no puede iniciarse de nuevo");
    }

    @Override
    public void finalizar(AccionesSesion acciones, Long idPractica, Long idDocente) {
        throw new ReglaNegocioException("La sesión ya finalizó");
    }

    @Override public List<String> accionesPermitidas() { return List.of(); }
}
