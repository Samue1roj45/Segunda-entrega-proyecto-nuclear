package com.example.demo.state;

import java.util.List;
/*
 #REQUISITO RF-19
 Patrón: STATE (interfaz del estado)
 Para qué sirve: cada estado de la sesión (SIN_INICIAR, EN_CURSO, FINALIZADO) decide
 qué acciones permite y cuáles rechaza. Reemplaza los if/else por estado dispersos.
 La BD (trigger trg_practicas_bu) sigue siendo la última barrera; este patrón da el
 mensaje claro y la lista de acciones permitidas para el frontend.
 */
public interface EstadoSesion {
    String nombre();
    void iniciar(AccionesSesion acciones, Long idPractica, Long idDocente);
    void finalizar(AccionesSesion acciones, Long idPractica, Long idDocente);
    List<String> accionesPermitidas();
}
