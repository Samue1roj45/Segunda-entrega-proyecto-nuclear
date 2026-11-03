package com.unihumboldt.nuclear.evaluacion.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class PublicadorEventosTest {

    @Test
    void notificaATodosYAcumulaEnviosDistintos() {
        List<EventoAcademico> recibidosA = new ArrayList<>();
        List<EventoAcademico> recibidosB = new ArrayList<>();
        ObservadorAcademico observadorA = evento -> {
            recibidosA.add(evento);
            return ResultadoEnvio.ENVIADA;
        };
        ObservadorAcademico observadorB = evento -> {
            recibidosB.add(evento);
            return ResultadoEnvio.ERROR;
        };

        PublicadorEventos publicador = new PublicadorEventos(List.of(observadorA, observadorB));
        EventoAcademico evento = evento("CONV:1:10");

        ResumenEnvio resumen = publicador.publicar(evento);

        assertEquals(1, recibidosA.size());
        assertEquals(1, recibidosB.size());
        assertEquals(evento, recibidosA.get(0));
        assertEquals(evento, recibidosB.get(0));
        assertEquals(new ResumenEnvio(1, 0, 1), resumen);
    }

    @Test
    void noSeQuedaSoloConElUltimoObservadorCuandoAmbosEnvian() {
        AtomicInteger llamadas = new AtomicInteger();
        ObservadorAcademico primero = evento -> {
            llamadas.incrementAndGet();
            return ResultadoEnvio.ENVIADA;
        };
        ObservadorAcademico segundo = evento -> {
            llamadas.incrementAndGet();
            return ResultadoEnvio.ENVIADA;
        };

        ResumenEnvio resumen = new PublicadorEventos(List.of(primero, segundo)).publicar(evento("CAL:3"));

        assertEquals(2, llamadas.get());
        assertEquals(2, resumen.enviadas());
        assertEquals(0, resumen.omitidasDuplicado());
        assertEquals(0, resumen.errores());
    }

    @Test
    void sinObservadoresNoInventaUnEnvioOmitido() {
        ResumenEnvio resumen = new PublicadorEventos(List.of()).publicar(evento("CAMBIO:1"));
        assertEquals(new ResumenEnvio(0, 0, 0), resumen);
    }

    private static EventoAcademico evento(String clave) {
        return new EventoAcademico(
                TipoNotificacion.CONVOCATORIA,
                clave,
                1L,
                10L,
                null,
                "asunto",
                "cuerpo");
    }
}
