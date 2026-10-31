package com.unihumboldt.nuclear.evaluacion.config;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.ActorActualResolver;
import com.unihumboldt.nuclear.evaluacion.contrato.Rol;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resuelve el actor desde cabeceras temporales.
 * Pendiente: sustituir por claims JWT del módulo de usuarios.
 *
 * Cabeceras: X-Usuario-Id, X-Rol, X-Estudiante-Id, X-Docente-Id
 */
@Component
public class ActorHeaderResolver implements ActorActualResolver {

    @Override
    public Optional<ActorActual> actual() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        HttpServletRequest request = attrs.getRequest();
        String rolValor = request.getHeader("X-Rol");
        String usuario = request.getHeader("X-Usuario-Id");
        if (rolValor == null || usuario == null) {
            return Optional.empty();
        }
        Rol rol;
        try {
            rol = Rol.valueOf(rolValor);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        Long estudianteId = parse(request.getHeader("X-Estudiante-Id"));
        Long docenteId = parse(request.getHeader("X-Docente-Id"));
        return Optional.of(new ActorActual(Long.valueOf(usuario), estudianteId, docenteId, rol));
    }

    private Long parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
