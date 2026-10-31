package com.unihumboldt.nuclear.evaluacion.participacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class MaquinaEstadoParticipacionTest {

    private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 10, 31, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime T1 = T0.plusMinutes(15);

    @Test
    void noIniciadaPermiteIniciar() {
        Participacion p = participacion(EstadoParticipacion.NO_INICIADA);
        MaquinaEstadoParticipacion.iniciar(p, T0);
        assertEquals(EstadoParticipacion.EN_CURSO, p.getEstado());
        assertEquals(T0, p.getInicio());
        assertNull(p.getFin());
        assertEquals(0, p.getTiempoUsadoSeg());
        assertSame(EstadoEnCurso.INSTANCIA, MaquinaEstadoParticipacion.de(p));
    }

    @Test
    void noFinalizaDesdeNoIniciada() {
        Participacion p = participacion(EstadoParticipacion.NO_INICIADA);
        assertThrows(TransicionInvalidaException.class,
                () -> MaquinaEstadoParticipacion.finalizar(p, T0));
        assertEquals(EstadoParticipacion.NO_INICIADA, p.getEstado());
    }

    @Test
    void noMarcaIncompletaDesdeNoIniciada() {
        Participacion p = participacion(EstadoParticipacion.NO_INICIADA);
        assertThrows(TransicionInvalidaException.class,
                () -> MaquinaEstadoParticipacion.marcarIncompleta(p, T0));
        assertEquals(EstadoParticipacion.NO_INICIADA, p.getEstado());
    }

    @Test
    void enCursoReusaElMismoIntentoAlIniciar() {
        Participacion p = participacion(EstadoParticipacion.EN_CURSO);
        p.setInicio(T0);
        MaquinaEstadoParticipacion.iniciar(p, T1);
        assertEquals(EstadoParticipacion.EN_CURSO, p.getEstado());
        assertEquals(T0, p.getInicio());
    }

    @Test
    void enCursoFinalizaYRegistraTiempo() {
        Participacion p = participacion(EstadoParticipacion.EN_CURSO);
        p.setInicio(T0);
        MaquinaEstadoParticipacion.finalizar(p, T1);
        assertEquals(EstadoParticipacion.FINALIZADA, p.getEstado());
        assertEquals(T1, p.getFin());
        assertEquals(15 * 60, p.getTiempoUsadoSeg());
        assertSame(EstadoFinalizada.INSTANCIA, MaquinaEstadoParticipacion.de(p));
    }

    @Test
    void enCursoPasaAIncompleta() {
        Participacion p = participacion(EstadoParticipacion.EN_CURSO);
        p.setInicio(T0);
        MaquinaEstadoParticipacion.marcarIncompleta(p, T1);
        assertEquals(EstadoParticipacion.INCOMPLETA, p.getEstado());
        assertEquals(T1, p.getFin());
        assertSame(EstadoIncompleta.INSTANCIA, MaquinaEstadoParticipacion.de(p));
    }

    @Test
    void finalizadaNoSeReabre() {
        Participacion p = participacion(EstadoParticipacion.FINALIZADA);
        p.setInicio(T0);
        p.setFin(T1);
        assertThrows(TransicionInvalidaException.class,
                () -> MaquinaEstadoParticipacion.iniciar(p, T1.plusMinutes(1)));
        MaquinaEstadoParticipacion.finalizar(p, T1.plusMinutes(1));
        assertEquals(EstadoParticipacion.FINALIZADA, p.getEstado());
        assertEquals(T1, p.getFin());
    }

    @Test
    void incompletaEsTerminalParaElIntento() {
        Participacion p = participacion(EstadoParticipacion.INCOMPLETA);
        assertThrows(TransicionInvalidaException.class,
                () -> MaquinaEstadoParticipacion.iniciar(p, T0));
        assertThrows(TransicionInvalidaException.class,
                () -> MaquinaEstadoParticipacion.finalizar(p, T0));
        assertThrows(TransicionInvalidaException.class,
                () -> MaquinaEstadoParticipacion.marcarIncompleta(p, T0));
        assertEquals(EstadoParticipacion.INCOMPLETA, p.getEstado());
    }

    @Test
    void cadaCodigoResuelveSuEstadoConcreto() {
        assertSame(EstadoNoIniciada.INSTANCIA,
                MaquinaEstadoParticipacion.de(participacion(EstadoParticipacion.NO_INICIADA)));
        assertSame(EstadoEnCurso.INSTANCIA,
                MaquinaEstadoParticipacion.de(participacion(EstadoParticipacion.EN_CURSO)));
        assertSame(EstadoFinalizada.INSTANCIA,
                MaquinaEstadoParticipacion.de(participacion(EstadoParticipacion.FINALIZADA)));
        assertSame(EstadoIncompleta.INSTANCIA,
                MaquinaEstadoParticipacion.de(participacion(EstadoParticipacion.INCOMPLETA)));
    }

    private static Participacion participacion(EstadoParticipacion estado) {
        Participacion p = new Participacion();
        p.setEstado(estado);
        return p;
    }
}
