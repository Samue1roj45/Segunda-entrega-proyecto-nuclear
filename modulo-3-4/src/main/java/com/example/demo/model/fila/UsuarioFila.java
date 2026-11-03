package com.example.demo.model.fila;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: una fila de la tabla `usuarios`. Sin password_hash a propósito: el hash nunca debe salir de la capa de autenticación.
 */
public record UsuarioFila(Long idUsuario, String nombreUsuario, String correo, String rol, Boolean activo, Long creadoPor, LocalDateTime fechaCreacion) {

    public static final RowMapper<UsuarioFila> MAPA = (rs, n) -> new UsuarioFila(
            Mapeo.largo(rs, "id_usuario"),
            Mapeo.texto(rs, "nombre_usuario"),
            Mapeo.texto(rs, "correoIns_usuario"),
            Mapeo.texto(rs, "rol"),
            Mapeo.bool(rs, "activo"),
            Mapeo.largo(rs, "creado_por"),
            Mapeo.fecha(rs, "fecha_creacion"));
}
