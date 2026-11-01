package com.example.demo.repository;

import com.example.demo.model.fila.UsuarioFila;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: acceso a `usuarios` y a sus procedimientos (RF-01/02/03). Todos los
 usuarios (administrador, docente, estudiante) viven en una sola tabla distinguidos por rol.
 Los métodos que cambian datos exigen un administrador y la BD lo valida (sp_assert_admin).
 */
@Repository
public class UsuarioRepository {

    private static final String SELECT = "SELECT id_usuario, nombre_usuario, correoIns_usuario, rol, activo, "
            + "creado_por, fecha_creacion FROM usuarios";

    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UsuarioFila> buscarPorId(Long id) {
        return jdbc.query(SELECT + " WHERE id_usuario = ?", UsuarioFila.MAPA, id).stream().findFirst();
    }

    public Optional<UsuarioFila> buscarPorCorreo(String correo) {
        return jdbc.query(SELECT + " WHERE correoIns_usuario = ?", UsuarioFila.MAPA, correo).stream().findFirst();
    }

    public List<UsuarioFila> listarPorRol(String rol) {
        return jdbc.query(SELECT + " WHERE rol = ? ORDER BY nombre_usuario", UsuarioFila.MAPA, rol);
    }

    /** fn_rol_usuario: rol del usuario activo, o null si no existe o está inactivo. */
    public String rolDe(Long idUsuario) {
        return jdbc.queryForObject("SELECT fn_rol_usuario(?)", String.class, idUsuario);
    }

    /** fn_tiene_permiso: permiso directo o heredado del rol. */
    public boolean tienePermiso(Long idUsuario, String nombrePermiso) {
        Boolean b = jdbc.queryForObject("SELECT fn_tiene_permiso(?, ?)", Boolean.class, idUsuario, nombrePermiso);
        return Boolean.TRUE.equals(b);
    }

    public Long crearDocente(Long idAdmin, String nombre, String correo, String passwordHash, boolean permisoCrearCasos) {
        return jdbc.queryForObject("CALL sp_crear_docente(?,?,?,?,?)", Long.class,
                idAdmin, nombre, correo, passwordHash, permisoCrearCasos ? 1 : 0);
    }

    public Long registrarEstudiante(Long idActor, String nombre, String correo) {
        return jdbc.queryForObject("CALL sp_registrar_estudiante(?,?,?)", Long.class, idActor, nombre, correo);
    }

    public void cambiarRol(Long idAdmin, Long idUsuario, String rol) {
        jdbc.update("CALL sp_cambiar_rol(?,?,?)", idAdmin, idUsuario, rol);
    }

    public void cambiarEstado(Long idAdmin, Long idUsuario, boolean activo) {
        jdbc.update("CALL sp_cambiar_estado_usuario(?,?,?)", idAdmin, idUsuario, activo ? 1 : 0);
    }

    public void asignarPermiso(Long idAdmin, Long idUsuario, String permiso) {
        jdbc.update("CALL sp_asignar_permiso(?,?,?)", idAdmin, idUsuario, permiso);
    }

    public void revocarPermiso(Long idAdmin, Long idUsuario, String permiso) {
        jdbc.update("CALL sp_revocar_permiso(?,?,?)", idAdmin, idUsuario, permiso);
    }
}
