package co.edu.humboldt.plataformacasos.sesiones.estado;

import java.util.List;
/*
 #REQUISITO RF-19
 Patrón: STATE (interfaz del estado)
 Para qué sirve: cada estado de la sesión (SIN_INICIAR, EN_CURSO, FINALIZADO) decide
 qué acciones permite y cuáles rechaza. Reemplaza los if/else por estado dispersos.
 Además entrega la lista de acciones permitidas para que el frontend sepa qué botones mostrar.
 */
public interface EstadoSesion {
    String nombre();
    void iniciar(AccionesSesion acciones, Long idPractica, Long idDocente);
    void finalizar(AccionesSesion acciones, Long idPractica, Long idDocente);
    List<String> accionesPermitidas();
}
