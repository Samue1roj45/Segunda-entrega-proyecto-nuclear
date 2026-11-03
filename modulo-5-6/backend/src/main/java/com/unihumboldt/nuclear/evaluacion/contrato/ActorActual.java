package com.unihumboldt.nuclear.evaluacion.contrato;

/**
 * Actor autenticado. Temporal hasta integrar JWT del módulo de usuarios.
 */
public record ActorActual(Long usuarioId, Long estudianteId, Long docenteId, Rol rol) {

    public boolean esEstudiante() {
        return rol == Rol.ESTUDIANTE;
    }

    public boolean esDocente() {
        return rol == Rol.DOCENTE;
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }
}
