package com.example.demo.model.fila;

import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `rol_permisos`. Permisos por defecto de cada rol.
 */
public record RolPermisoFila(String rol, Integer idPermiso) {

    public static final RowMapper<RolPermisoFila> MAPA = (rs, n) -> new RolPermisoFila(
            Mapeo.texto(rs, "rol"),
            Mapeo.entero(rs, "id_permiso"));
}
