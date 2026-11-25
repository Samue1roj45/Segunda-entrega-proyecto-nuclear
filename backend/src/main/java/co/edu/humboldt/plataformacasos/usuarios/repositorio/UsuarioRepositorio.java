package co.edu.humboldt.plataformacasos.usuarios.repositorio;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);

    boolean existsByCorreoInstitucional(String correoInstitucional);

    List<Usuario> findByRolOrderByNombreCompletoAsc(Rol rol);

    /** Administración de cuentas: activas o archivadas, con filtro opcional por rol. */
    @Query("""
            select u from Usuario u
             where u.activo = :activo and (:rol is null or u.rol = :rol)
             order by u.rol asc, u.nombreCompleto asc""")
    List<Usuario> listarPorEstado(@Param("activo") boolean activo, @Param("rol") Rol rol);

    /** Bloquea la cuenta hasta el final de la transacción para que dos administradores no la cambien a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaModificar(@Param("id") Long id);

    /**
     * Administradores activos, bloqueados hasta el final de la transacción: dos administradores que se
     * archivan entre sí al mismo tiempo no pueden dejar el sistema sin ninguno.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.rol = co.edu.humboldt.plataformacasos.usuarios.dominio.Rol.ADMINISTRADOR and u.activo = true")
    List<Usuario> bloquearAdministradoresActivos();
}
