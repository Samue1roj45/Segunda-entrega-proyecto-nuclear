package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `autorizaciones_repeticion`. RF-25 Cada fila concede un intento extra.*/
public record AutorizacionRepeticionFila(Long idRepeticion, Long idAutorizacion, Long idDocenteAutoriza, String motivo, LocalDateTime fechaAutorizacion) {

    public static final RowMapper<AutorizacionRepeticionFila> MAPA = (rs, n) -> new AutorizacionRepeticionFila(
            Mapeo.largo(rs, "id_repeticion"),
            Mapeo.largo(rs, "id_autorizacion"),
            Mapeo.largo(rs, "id_docente_autoriza"),
            Mapeo.texto(rs, "motivo"),
            Mapeo.fecha(rs, "fecha_autorizacion"));
}
