package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `versiones_dsl_caso`. RF-28 Documento DSL del caso (herramientas y estructura) con su validación.
 */
public record VersionDslCasoFila(Long idVersionDsl, Long idCaso, String nombreDsl, String versionDsl, String documentoJson, String estadoValidacion, String erroresValidacion, LocalDateTime fechaCreacion) {

    public static final RowMapper<VersionDslCasoFila> MAPA = (rs, n) -> new VersionDslCasoFila(
            Mapeo.largo(rs, "id_version_dsl"),
            Mapeo.largo(rs, "id_caso"),
            Mapeo.texto(rs, "nombre_dsl"),
            Mapeo.texto(rs, "version_dsl"),
            Mapeo.texto(rs, "documento_json"),
            Mapeo.texto(rs, "estado_validacion"),
            Mapeo.texto(rs, "errores_validacion"),
            Mapeo.fecha(rs, "fecha_creacion"));
}
