package com.example.demo.model.fila;

import java.math.BigDecimal;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `rubricas`. Rúbrica 1:1 con el caso.
 */
public record RubricaFila(Long idRubrica, Long idCaso, String nombreRubrica, String descripcionRubrica, BigDecimal notaMaxima) {

    public static final RowMapper<RubricaFila> MAPA = (rs, n) -> new RubricaFila(
            Mapeo.largo(rs, "id_rubrica"),
            Mapeo.largo(rs, "id_caso"),
            Mapeo.texto(rs, "nombre_rubrica"),
            Mapeo.texto(rs, "descripcion_rubrica"),
            Mapeo.decimal(rs, "nota_maxima"));
}
