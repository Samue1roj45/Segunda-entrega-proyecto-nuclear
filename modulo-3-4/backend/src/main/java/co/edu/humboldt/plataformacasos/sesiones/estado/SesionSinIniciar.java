package co.edu.humboldt.plataformacasos.sesiones.estado;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import java.util.List;
/*
 #REQUISITO RF-19
 Patrón: STATE (estado concreto). SIN_INICIAR -> solo se puede INICIAR.
 */
public final class SesionSinIniciar implements EstadoSesion {

    @Override public String nombre() { return Practica.SIN_INICIAR; }

    @Override
    public void iniciar(AccionesSesion acciones, Long idPractica, Long idDocente) {
        acciones.iniciarEnBd(idPractica, idDocente);      // -> EN_CURSO
    }

    @Override
    public void finalizar(AccionesSesion acciones, Long idPractica, Long idDocente) {
        throw new ReglaNegocioException("La sesión aún no se ha iniciado; no puede finalizarse");
    }

    @Override public List<String> accionesPermitidas() { return List.of("INICIAR"); }
}
