package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;

import java.util.Set;
import java.util.TreeSet;

/** Vista de un docente para la API. No expone hashes ni datos internos de la entidad. */
public record DocenteDTO(Long id, String correoInstitucional, String nombreCompleto, boolean activo,
                         Set<Permiso> permisos) {

    public static DocenteDTO de(Usuario docente) {
        return new DocenteDTO(docente.getId(), docente.getCorreoInstitucional(), docente.getNombreCompleto(),
                docente.isActivo(), new TreeSet<>(docente.getPermisosAsignados()));
    }
}
