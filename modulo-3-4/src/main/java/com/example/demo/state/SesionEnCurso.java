package com.example.demo.state;

import com.example.demo.exception.ReglaNegocioException;
import java.util.List;
/*
 #REQUISITO RF-19
 Patrón: STATE (estado concreto). EN_CURSO -> solo se puede FINALIZAR.
 Al finalizar, la BD cierra los intentos abiertos, marca ausentes y califica.
 */
public final class SesionEnCurso implements EstadoSesion {

    @Override public String nombre() { return "EN_CURSO"; }

    @Override
    public void iniciar(AccionesSesion acciones, Long idPractica, Long idDocente) {
        throw new ReglaNegocioException("La sesión ya está en curso");
    }

    @Override
    public void finalizar(AccionesSesion acciones, Long idPractica, Long idDocente) {
        acciones.finalizarEnBd(idPractica, idDocente);    // -> FINALIZADO (cierre MANUAL)
    }

    @Override public List<String> accionesPermitidas() { return List.of("FINALIZAR"); }
}
