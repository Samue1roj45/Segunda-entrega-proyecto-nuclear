package com.unihumboldt.nuclear.evaluacion.participacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.Rol;

@ExtendWith(MockitoExtension.class)
class ParticipacionConsultaProxyTest {

    @Mock
    private ParticipacionFacade real;

    @Mock
    private AccesoEvaluacion acceso;

    private ParticipacionConsultaProxy proxy;

    @BeforeEach
    void setup() {
        proxy = new ParticipacionConsultaProxy(real, acceso);
    }

    @Test
    void permiteConsultaDelDueno() {
        ActorActual estudiante = new ActorActual(10L, 10L, null, Rol.ESTUDIANTE);
        Participacion participacion = new Participacion();
        participacion.setEstudianteId(10L);
        participacion.setPracticaId(1L);
        ParticipacionRespuesta dto = respuesta(1L, 1L, 10L);
        when(acceso.exigirActor()).thenReturn(estudiante);
        when(acceso.exigirParticipacion(1L)).thenReturn(participacion);
        when(real.describir(participacion)).thenReturn(dto);

        assertEquals(dto, proxy.obtener(1L));
        verify(acceso).exigirVisible(participacion, estudiante);
        verify(real).describir(participacion);
    }

    @Test
    void bloqueaParticipacionAjenaAntesDeDescribir() {
        ActorActual otro = new ActorActual(11L, 11L, null, Rol.ESTUDIANTE);
        Participacion participacion = new Participacion();
        when(acceso.exigirActor()).thenReturn(otro);
        when(acceso.exigirParticipacion(1L)).thenReturn(participacion);
        org.mockito.Mockito.doThrow(new RecursoNoEncontradoException("Participación no encontrada."))
                .when(acceso).exigirVisible(participacion, otro);

        assertThrows(RecursoNoEncontradoException.class, () -> proxy.obtener(1L));
        verify(real, never()).describir(any());
    }

    @Test
    void listaSoloFilasVisibles() {
        ActorActual docente = new ActorActual(5L, null, 5L, Rol.DOCENTE);
        when(acceso.exigirActor()).thenReturn(docente);
        when(real.listar(null, null, null, null, null, null)).thenReturn(List.of(
                respuesta(1L, 1L, 10L),
                respuesta(2L, 9L, 11L)));
        when(acceso.visible(docente, 1L, 10L)).thenReturn(true);
        when(acceso.visible(docente, 9L, 11L)).thenReturn(false);

        List<ParticipacionRespuesta> visibles = proxy.listar(null, null, null, null, null, null);
        assertEquals(1, visibles.size());
        assertEquals(1L, visibles.get(0).id());
        assertTrue(visibles.stream().noneMatch(p -> p.practicaId() == 9L));
    }

    private static ParticipacionRespuesta respuesta(long id, long practicaId, long estudianteId) {
        return new ParticipacionRespuesta(
                id, practicaId, "P", estudianteId, "a@b.c", "N", 1L, 1, null, null, 0,
                EstadoParticipacion.FINALIZADA);
    }
}
