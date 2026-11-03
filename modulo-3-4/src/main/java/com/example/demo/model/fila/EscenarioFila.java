package com.example.demo.model.fila;

import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `escenarios`. Escenarios ordenados de un caso.*/
public record EscenarioFila(Long idEscenario, Long idCaso, Integer ordenEscenario, String nombreEscenario, String contextoEscenario) {

    public static final RowMapper<EscenarioFila> MAPA = (rs, n) -> new EscenarioFila(
            Mapeo.largo(rs, "id_escenario"),
            Mapeo.largo(rs, "id_caso"),
            Mapeo.entero(rs, "orden_escenario"),
            Mapeo.texto(rs, "nombre_escenario"),
            Mapeo.texto(rs, "contexto_escenario"));
}
