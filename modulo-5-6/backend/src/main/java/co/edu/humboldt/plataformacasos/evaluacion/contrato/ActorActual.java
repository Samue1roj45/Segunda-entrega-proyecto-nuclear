package co.edu.humboldt.plataformacasos.evaluacion.contrato;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;

import java.util.Set;

/**
 * Usuario autenticado (token del módulo 1) visto desde este módulo. En la base única los estudiantes
 * y docentes son usuarios del módulo 1, así que estudianteId y docenteId son el mismo id de usuario
 * según el rol. Los permisos son los vigentes en la base (RF-02, RF-03).
 */
public record ActorActual(Long usuarioId, Long estudianteId, Long docenteId, Rol rol, Set<Permiso> permisos) {

    public ActorActual {
        permisos = permisos == null ? Set.of() : Set.copyOf(permisos);
    }

    /** Actor con solo los permisos base de su rol (sin permisos asignados por el administrador). */
    public ActorActual(Long usuarioId, Long estudianteId, Long docenteId, Rol rol) {
        this(usuarioId, estudianteId, docenteId, rol, rol == null ? Set.of() : rol.permisosBase());
    }

    public boolean esEstudiante() {
        return rol == Rol.ESTUDIANTE;
    }

    public boolean esDocente() {
        return rol == Rol.DOCENTE;
    }

    public boolean esAdmin() {
        return rol == Rol.ADMINISTRADOR;
    }

    public boolean tienePermiso(Permiso permiso) {
        return permisos.contains(permiso);
    }
}
