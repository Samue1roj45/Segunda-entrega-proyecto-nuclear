package com.unihumboldt.nuclear.evaluacion.config;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.ActorActualResolver;
import com.unihumboldt.nuclear.evaluacion.contrato.Rol;

/**
 * El actor sale de los claims del JWT ya validado.
 * Las cabeceras X-Rol, X-Usuario-Id, X-Estudiante-Id y X-Docente-Id no se leen.
 *
 * Claims: rol (ESTUDIANTE, DOCENTE o ADMIN), usuarioId, estudianteId y docenteId.
 * Si falta usuarioId se acepta sub numérico.
 */
@Component
public class ActorJwtResolver implements ActorActualResolver {

    @Override
    public Optional<ActorActual> actual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        try {
            String rolValor = texto(jwt.getClaim("rol"));
            if (rolValor == null) {
                return Optional.empty();
            }
            Rol rol = Rol.valueOf(rolValor);
            Long usuarioId = numero(jwt.getClaim("usuarioId"));
            if (usuarioId == null) {
                usuarioId = numero(jwt.getSubject());
            }
            if (usuarioId == null) {
                return Optional.empty();
            }
            return Optional.of(new ActorActual(
                    usuarioId,
                    numero(jwt.getClaim("estudianteId")),
                    numero(jwt.getClaim("docenteId")),
                    rol));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private static Long numero(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        String texto = valor.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        return Long.valueOf(texto);
    }
}
