package com.unihumboldt.nuclear.evaluacion.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.Rol;

class ActorJwtResolverTest {

    private final ActorJwtResolver resolver = new ActorJwtResolver();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void leeClaimsDelJwt() {
        autenticar(Map.of("rol", "ESTUDIANTE", "usuarioId", 7L, "estudianteId", 10L));

        Optional<ActorActual> actor = resolver.actual();

        assertTrue(actor.isPresent());
        assertEquals(7L, actor.get().usuarioId());
        assertEquals(10L, actor.get().estudianteId());
        assertNull(actor.get().docenteId());
        assertEquals(Rol.ESTUDIANTE, actor.get().rol());
    }

    @Test
    void sinJwtNoHayActor() {
        assertTrue(resolver.actual().isEmpty());
    }

    @Test
    void rolDesconocidoOSinUsuarioNoHayActor() {
        autenticar(Map.of("rol", "SUPERUSUARIO", "usuarioId", 7L));
        assertTrue(resolver.actual().isEmpty());

        autenticar(Map.of("rol", "DOCENTE"));
        assertTrue(resolver.actual().isEmpty());
    }

    @Test
    void usuarioNoNumericoNoHayActor() {
        autenticar(Map.of("rol", "ESTUDIANTE", "usuarioId", "abc", "estudianteId", 10L));
        assertTrue(resolver.actual().isEmpty());
    }

    @Test
    void usaSubSiFaltaUsuarioId() {
        autenticar(Map.of("rol", "DOCENTE", "sub", "5", "docenteId", 5));

        Optional<ActorActual> actor = resolver.actual();

        assertTrue(actor.isPresent());
        assertEquals(5L, actor.get().usuarioId());
        assertEquals(5L, actor.get().docenteId());
    }

    private static void autenticar(Map<String, Object> claims) {
        Jwt jwt = new Jwt(
                "token-de-prueba",
                Instant.parse("2026-11-01T00:00:00Z"),
                Instant.parse("2026-11-01T01:00:00Z"),
                Map.of("alg", "HS256"),
                claims);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
