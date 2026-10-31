package com.unihumboldt.nuclear.evaluacion.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.Rol;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.EstadoParticipacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;

@ExtendWith(MockitoExtension.class)
class ResultadoConsultaProxyTest {

    @Mock
    private EvaluacionFacade real;

    @Mock
    private AccesoEvaluacion acceso;

    private ResultadoConsultaProxy proxy;

    @BeforeEach
    void setup() {
        proxy = new ResultadoConsultaProxy(real, acceso);
    }

    @Test
    void permiteResultadoPropioSinCambiarNota() {
        ActorActual estudiante = new ActorActual(10L, 10L, null, Rol.ESTUDIANTE);
        Resultado resultado = resultadoDe(10L, 1L);
        ResultadoRespuesta dto = respuesta(8L, 10L, 1L, new BigDecimal("50.00"));
        when(acceso.exigirActor()).thenReturn(estudiante);
        when(real.exigirResultado(8L)).thenReturn(resultado);
        when(acceso.visible(estudiante, resultado.getParticipacion())).thenReturn(true);
        when(real.consultarCerrado(resultado)).thenReturn(dto);

        ResultadoRespuesta visto = proxy.obtener(8L);
        assertSame(dto, visto);
        assertEquals(new BigDecimal("50.00"), visto.notaFinal());
        verify(real).consultarCerrado(resultado);
    }

    @Test
    void ocultaResultadoAjenoCon404YNoConsultaNota() {
        ActorActual otro = new ActorActual(11L, 11L, null, Rol.ESTUDIANTE);
        Resultado resultado = resultadoDe(10L, 1L);
        when(acceso.exigirActor()).thenReturn(otro);
        when(real.exigirResultado(8L)).thenReturn(resultado);
        when(acceso.visible(otro, resultado.getParticipacion())).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> proxy.obtener(8L));
        verify(real, never()).consultarCerrado(any());
    }

    @Test
    void listaNotasOficialesSoloVisibles() {
        ActorActual docente = new ActorActual(5L, null, 5L, Rol.DOCENTE);
        when(acceso.exigirActor()).thenReturn(docente);
        when(real.listarOficiales(null, null)).thenReturn(List.of(
                new NotaOficialRespuesta(1L, 1L, 10L, 3L, 1, new BigDecimal("0.00"), 5L, null),
                new NotaOficialRespuesta(2L, 9L, 11L, 4L, 1, new BigDecimal("80.00"), 99L, null)));
        when(acceso.visible(docente, 1L, 10L)).thenReturn(true);
        when(acceso.visible(docente, 9L, 11L)).thenReturn(false);

        List<NotaOficialRespuesta> visibles = proxy.listarOficiales(null, null);
        assertEquals(1, visibles.size());
        assertEquals(new BigDecimal("0.00"), visibles.get(0).notaOficial());
    }

    private static Resultado resultadoDe(long estudianteId, long practicaId) {
        Participacion p = new Participacion();
        p.setEstudianteId(estudianteId);
        p.setPracticaId(practicaId);
        p.setEstado(EstadoParticipacion.FINALIZADA);
        Resultado r = new Resultado();
        r.setParticipacion(p);
        r.setNotaFinal(new BigDecimal("50.00"));
        return r;
    }

    private static ResultadoRespuesta respuesta(long id, long estudianteId, long practicaId, BigDecimal nota) {
        return new ResultadoRespuesta(
                id, 3L, 1, estudianteId, "a@b.c", "N", practicaId, "P",
                EstadoParticipacion.FINALIZADA, 1, 1, 0, 2, 4, 100, nota, false, null, null, List.of());
    }
}
