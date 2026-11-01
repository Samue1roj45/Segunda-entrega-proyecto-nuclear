package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `autorizaciones`. Contiene el código de autorización: NO enviarla tal cual a otros usuarios.*/
public record AutorizacionFila(Long idAutorizacion, Long idPractica, Long idEstudiante, String codigoAutorizacion, LocalDateTime vigenciaHasta, String estado, LocalDateTime fechaAsignacion, LocalDateTime fechaRetiro) {

    public static final RowMapper<AutorizacionFila> MAPA = (rs, n) -> new AutorizacionFila(
            Mapeo.largo(rs, "id_autorizacion"),
            Mapeo.largo(rs, "id_practica"),
            Mapeo.largo(rs, "id_estudiante"),
            Mapeo.texto(rs, "codigo_autorizacion"),
            Mapeo.fecha(rs, "vigencia_hasta"),
            Mapeo.texto(rs, "estado"),
            Mapeo.fecha(rs, "fecha_asignacion"),
            Mapeo.fecha(rs, "fecha_retiro"));
}
