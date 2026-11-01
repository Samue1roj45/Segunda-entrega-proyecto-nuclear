package com.example.demo.state;
/*
 #REQUISITO RF-19
 Patrón: STATE (colaborador)
 Para qué sirve: operaciones reales que ejecutan los estados (las implementa el
 repositorio con los procedimientos de la BD). Así los estados no conocen JDBC.
 */
public interface AccionesSesion {
    void iniciarEnBd(Long idPractica, Long idDocente);
    void finalizarEnBd(Long idPractica, Long idDocente);
}
