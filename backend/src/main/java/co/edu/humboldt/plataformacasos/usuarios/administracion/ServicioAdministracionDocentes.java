package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.util.List;
import java.util.Set;

/**
 * RF-03: el administrador crea docentes y les asigna permisos. Cada operación declara el permiso
 * que exige; {@link co.edu.humboldt.plataformacasos.autorizacion.ProxyAcceso} lo verifica antes de delegar.
 */
public interface ServicioAdministracionDocentes {

    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    DocenteDTO crearDocente(CrearDocenteRequest datos);

    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    List<DocenteDTO> listarDocentes();

    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    DocenteDTO asignarPermisos(Long docenteId, Set<Permiso> permisos);
}
