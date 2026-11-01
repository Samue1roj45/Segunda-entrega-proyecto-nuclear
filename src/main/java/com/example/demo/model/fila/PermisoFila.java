package com.example.demo.model.fila;

import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `permisos`. Catálogo de permisos.
 */
public record PermisoFila(Integer idPermiso, String nombrePermiso, String descripcionPermiso) {

    public static final RowMapper<PermisoFila> MAPA = (rs, n) -> new PermisoFila(
            Mapeo.entero(rs, "id_permiso"),
            Mapeo.texto(rs, "nombre_permiso"),
            Mapeo.texto(rs, "descripcion_permiso"));
}
