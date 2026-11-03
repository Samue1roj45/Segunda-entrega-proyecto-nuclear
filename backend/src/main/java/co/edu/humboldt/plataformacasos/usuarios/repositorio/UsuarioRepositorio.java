package co.edu.humboldt.plataformacasos.usuarios.repositorio;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);

    boolean existsByCorreoInstitucional(String correoInstitucional);

    List<Usuario> findByRolOrderByNombreCompletoAsc(Rol rol);
}
