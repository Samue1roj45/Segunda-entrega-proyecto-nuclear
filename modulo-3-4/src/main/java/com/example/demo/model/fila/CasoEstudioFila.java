package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `casos_estudio`. RF-05/09/28 Caso de estudio.*/
public record CasoEstudioFila(Long idCaso, Long idDocenteCreador, String nombreCaso, String contextoHistoria, String desarrolloSituacional, Integer tiempoEstimadoMinutos, String estadoCaso, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion, LocalDateTime fechaPublicacion, LocalDateTime fechaArchivado) {

    public static final RowMapper<CasoEstudioFila> MAPA = (rs, n) -> new CasoEstudioFila(
            Mapeo.largo(rs, "id_caso"),
            Mapeo.largo(rs, "id_docente_creador"),
            Mapeo.texto(rs, "nombre_caso"),
            Mapeo.texto(rs, "contexto_historia"),
            Mapeo.texto(rs, "desarrollo_situacional"),
            Mapeo.entero(rs, "tiempo_estimado_minutos"),
            Mapeo.texto(rs, "estado_caso"),
            Mapeo.fecha(rs, "fecha_creacion"),
            Mapeo.fecha(rs, "fecha_actualizacion"),
            Mapeo.fecha(rs, "fecha_publicacion"),
            Mapeo.fecha(rs, "fecha_archivado"));
}
