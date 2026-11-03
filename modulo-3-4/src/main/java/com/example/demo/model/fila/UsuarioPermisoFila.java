package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `usuario_permisos`. Permisos asignados a un usuario.
 */
public record UsuarioPermisoFila(Long idUsuario, Integer idPermiso, Long otorgadoPor, LocalDateTime fechaOtorgado) {

    public static final RowMapper<UsuarioPermisoFila> MAPA = (rs, n) -> new UsuarioPermisoFila(
            Mapeo.largo(rs, "id_usuario"),
            Mapeo.entero(rs, "id_permiso"),
            Mapeo.largo(rs, "otorgado_por"),
            Mapeo.fecha(rs, "fecha_otorgado"));
}
