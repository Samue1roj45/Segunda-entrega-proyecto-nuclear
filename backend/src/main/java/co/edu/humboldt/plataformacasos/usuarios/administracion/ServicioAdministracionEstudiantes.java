package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.util.List;

/**
 * El administrador registra a los estudiantes (nombre y correo institucional) para que los docentes puedan
 * convocarlos (RF-04). El estudiante no tiene contraseña: ingresa con su correo y el código de cada
 * convocatoria (RF-01 + RF-20). Mismo permiso y mismo {@link co.edu.humboldt.plataformacasos.autorizacion.ProxyAcceso}
 * que la administración de docentes (RF-03).
 */
public interface ServicioAdministracionEstudiantes {

    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    EstudianteDTO crearEstudiante(CrearEstudianteRequest datos);

    @RequierePermiso(Permiso.ADMINISTRAR_DOCENTES)
    List<EstudianteDTO> listarEstudiantes();
}
