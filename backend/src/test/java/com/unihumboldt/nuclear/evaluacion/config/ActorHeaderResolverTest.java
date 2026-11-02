package com.unihumboldt.nuclear.evaluacion.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.Rol;

class ActorHeaderResolverTest {

    private final ActorHeaderResolver resolver = new ActorHeaderResolver();

    @AfterEach
    void limpiarContexto() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resuelveActorConCabecerasValidas() {
        peticion("ESTUDIANTE", "7", "10", null);

        Optional<ActorActual> actor = resolver.actual();

        assertTrue(actor.isPresent());
        assertEquals(7L, actor.get().usuarioId());
        assertEquals(10L, actor.get().estudianteId());
        assertNull(actor.get().docenteId());
        assertEquals(Rol.ESTUDIANTE, actor.get().rol());
    }

    @Test
    void sinPeticionNoHayActor() {
        assertTrue(resolver.actual().isEmpty());
    }

    @Test
    void sinRolOUsuarioNoHayActor() {
        peticion(null, "7", null, null);
        assertTrue(resolver.actual().isEmpty());

        peticion("DOCENTE", null, null, "5");
        assertTrue(resolver.actual().isEmpty());
    }

    @Test
    void rolDesconocidoNoHayActor() {
        peticion("SUPERUSUARIO", "7", null, null);
        assertTrue(resolver.actual().isEmpty());
    }

    @Test
    void usuarioNoNumericoNoLanzaExcepcion() {
        peticion("ESTUDIANTE", "abc", "10", null);
        assertTrue(resolver.actual().isEmpty());
    }

    @Test
    void estudianteODocenteNoNumericoNoLanzaExcepcion() {
        peticion("ESTUDIANTE", "7", "diez", null);
        assertTrue(resolver.actual().isEmpty());

        peticion("DOCENTE", "7", null, "5; DROP");
        assertTrue(resolver.actual().isEmpty());
    }

    private static void peticion(String rol, String usuario, String estudiante, String docente) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (rol != null) {
            request.addHeader("X-Rol", rol);
        }
        if (usuario != null) {
            request.addHeader("X-Usuario-Id", usuario);
        }
        if (estudiante != null) {
            request.addHeader("X-Estudiante-Id", estudiante);
        }
        if (docente != null) {
            request.addHeader("X-Docente-Id", docente);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
