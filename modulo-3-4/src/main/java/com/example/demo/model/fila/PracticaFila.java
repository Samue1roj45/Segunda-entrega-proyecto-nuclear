package com.example.demo.model.fila;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `practicas`. Sesión programada de un caso.
 */
public record PracticaFila(Long idPractica, Long idCaso, Long idDocente, LocalDateTime fechaInicio, LocalDateTime fechaFin, Integer tiempoMaximoMinutos, String espacioAcademico, String lugar, String mensajePersonalizado, String urlAcceso, String estadoPractica, LocalDateTime fechaInicioReal, LocalDateTime fechaCierreReal, String tipoCierre, BigDecimal porcentajeLimiteIngresoTardio) {

    public static final RowMapper<PracticaFila> MAPA = (rs, n) -> new PracticaFila(
            Mapeo.largo(rs, "id_practica"),
            Mapeo.largo(rs, "id_caso"),
            Mapeo.largo(rs, "id_docente"),
            Mapeo.fecha(rs, "fecha_inicio"),
            Mapeo.fecha(rs, "fecha_fin"),
            Mapeo.entero(rs, "tiempo_maximo_minutos"),
            Mapeo.texto(rs, "espacio_academico"),
            Mapeo.texto(rs, "lugar"),
            Mapeo.texto(rs, "mensaje_personalizado"),
            Mapeo.texto(rs, "url_acceso"),
            Mapeo.texto(rs, "estado_practica"),
            Mapeo.fecha(rs, "fecha_inicio_real"),
            Mapeo.fecha(rs, "fecha_cierre_real"),
            Mapeo.texto(rs, "tipo_cierre"),
            Mapeo.decimal(rs, "porcentaje_limite_ingreso_tardio"));
}
