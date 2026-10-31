package com.unihumboldt.nuclear.evaluacion.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoDenegadoException;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;

@ExtendWith(MockitoExtension.class)
class RetroalimentacionProxyTest {

    @Mock
    private RetroalimentacionFacade real;

    @Mock
    private AccesoEvaluacion acceso;

    private RetroalimentacionProxy proxy;

    @BeforeEach
    void setup() {
        proxy = new RetroalimentacionProxy(real, acceso);
    }

    @Test
    void estudianteNoRegistraRetroalimentacion() {
        when(acceso.exigirActor()).thenReturn(new ActorActual(10L, 10L, null, Rol.ESTUDIANTE));
        RegistrarRetroalimentacionRequest request = new RegistrarRetroalimentacionRequest(1L, "Bien");
        assertThrows(AccesoDenegadoException.class, () -> proxy.registrar(request));
        verify(real, never()).guardar(any(), any(), any());
    }

    @Test
    void docenteDeLaPracticaPuedeRegistrar() {
        ActorActual docente = new ActorActual(5L, null, 5L, Rol.DOCENTE);
        Participacion participacion = new Participacion();
        participacion.setPracticaId(1L);
        RegistrarRetroalimentacionRequest request = new RegistrarRetroalimentacionRequest(1L, "Revisa el isotopo.");
        RetroalimentacionRespuesta dto = new RetroalimentacionRespuesta(
                9L, 1L, 1, 10L, 1L, 5L, "Revisa el isotopo.", null, null);
        when(acceso.exigirActor()).thenReturn(docente);
        when(acceso.exigirParticipacion(1L)).thenReturn(participacion);
        when(real.guardar(docente, participacion, request)).thenReturn(dto);

        assertEquals(dto, proxy.registrar(request));
        verify(acceso).exigirPracticaDocente(1L, docente);
        verify(real).guardar(docente, participacion, request);
    }

    @Test
    void estudianteNoConsultaRetroalimentacionAjena() {
        ActorActual otro = new ActorActual(11L, 11L, null, Rol.ESTUDIANTE);
        Participacion participacion = new Participacion();
        Retroalimentacion retro = new Retroalimentacion();
        retro.setParticipacion(participacion);
        when(acceso.exigirActor()).thenReturn(otro);
        when(real.exigir(9L)).thenReturn(retro);
        when(acceso.visible(otro, participacion)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> proxy.obtener(9L));
        verify(real, never()).describir(any());
    }

    @Test
    void listaSoloRetroalimentacionesVisibles() {
        ActorActual estudiante = new ActorActual(10L, 10L, null, Rol.ESTUDIANTE);
        when(acceso.exigirActor()).thenReturn(estudiante);
        when(real.listar(null, null)).thenReturn(List.of(
                new RetroalimentacionRespuesta(1L, 1L, 1, 10L, 1L, 5L, "propia", null, null),
                new RetroalimentacionRespuesta(2L, 2L, 1, 11L, 1L, 5L, "ajena", null, null)));
        when(acceso.visible(estudiante, 1L, 10L)).thenReturn(true);
        when(acceso.visible(estudiante, 1L, 11L)).thenReturn(false);

        List<RetroalimentacionRespuesta> visibles = proxy.listar(null, null);
        assertEquals(1, visibles.size());
        assertEquals("propia", visibles.get(0).texto());
    }
}
