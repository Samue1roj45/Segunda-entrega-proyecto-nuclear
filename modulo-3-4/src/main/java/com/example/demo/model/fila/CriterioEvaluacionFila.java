package com.example.demo.model.fila;

import java.math.BigDecimal;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `criterios_evaluacion`. Criterios de la rúbrica (pesos suman 100).*/
public record CriterioEvaluacionFila(Long idCriterio, Long idRubrica, Integer ordenCriterio, String nombreCriterio, String parametrosCriterio, BigDecimal pesoPorcentaje) {

    public static final RowMapper<CriterioEvaluacionFila> MAPA = (rs, n) -> new CriterioEvaluacionFila(
            Mapeo.largo(rs, "id_criterio"),
            Mapeo.largo(rs, "id_rubrica"),
            Mapeo.entero(rs, "orden_criterio"),
            Mapeo.texto(rs, "nombre_criterio"),
            Mapeo.texto(rs, "parametros_criterio"),
            Mapeo.decimal(rs, "peso_porcentaje"));
}
