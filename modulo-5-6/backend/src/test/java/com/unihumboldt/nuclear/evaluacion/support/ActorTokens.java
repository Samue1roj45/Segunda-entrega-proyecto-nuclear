package com.unihumboldt.nuclear.evaluacion.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/**
 * JWT de prueba. El rol y los ids van en claims, no en cabeceras.
 */
public final class ActorTokens {

    private ActorTokens() {
    }

    public static JwtRequestPostProcessor estudiante(long id) {
        return token("ESTUDIANTE", id, id, null);
    }

    public static JwtRequestPostProcessor docente(long id) {
        return token("DOCENTE", id, null, id);
    }

    private static JwtRequestPostProcessor token(String rol, long usuarioId, Long estudianteId, Long docenteId) {
        return jwt().jwt(builder -> {
            builder.subject(Long.toString(usuarioId))
                    .claim("usuarioId", usuarioId)
                    .claim("rol", rol);
            if (estudianteId != null) {
                builder.claim("estudianteId", estudianteId);
            }
            if (docenteId != null) {
                builder.claim("docenteId", docenteId);
            }
        });
    }
}
