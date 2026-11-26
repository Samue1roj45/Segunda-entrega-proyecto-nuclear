package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;

/** Vista de un estudiante para el administrador. No expone hashes ni datos internos de la entidad. */
public record EstudianteDTO(Long id, String correoInstitucional, String nombreCompleto, boolean activo) {

    public static EstudianteDTO de(Usuario estudiante) {
        return new EstudianteDTO(estudiante.getId(), estudiante.getCorreoInstitucional(),
                estudiante.getNombreCompleto(), estudiante.isActivo());
    }
}
