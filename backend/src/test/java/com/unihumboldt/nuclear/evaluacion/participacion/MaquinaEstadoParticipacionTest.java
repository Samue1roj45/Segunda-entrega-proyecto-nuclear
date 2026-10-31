package com.unihumboldt.nuclear.evaluacion.participacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class MaquinaEstadoParticipacionTest {

    @Test
    void noFinalizaDesdeNoIniciada() {
        Participacion p = new Participacion();
        p.setEstado(EstadoParticipacion.NO_INICIADA);
        assertThrows(TransicionInvalidaException.class,
                () -> MaquinaEstadoParticipacion.finalizar(p, OffsetDateTime.now(ZoneOffset.UTC)));
        assertEquals(EstadoParticipacion.NO_INICIADA, p.getEstado());
    }
}
