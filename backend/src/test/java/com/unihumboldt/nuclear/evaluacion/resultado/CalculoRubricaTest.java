package com.unihumboldt.nuclear.evaluacion.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademicoEnMemoria;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PreguntaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.RespuestaOpcionRef;
import com.unihumboldt.nuclear.evaluacion.contrato.RubricaRef;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionada;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionadaRepository;

class CalculoRubricaTest {

    @Test
    void ausenciaNotaCeroAunqueHayaPreguntas() {
        CatalogoAcademicoEnMemoria catalogo = new CatalogoAcademicoEnMemoria();
        catalogo.registrar(new PracticaRef(1L, "P", 9L, 5L, null, null, "EN_CURSO", ""));
        catalogo.registrar(new PreguntaRef(1L, 9L, "P1", 3));
        catalogo.registrar(new RubricaRef(1L, 9L, 80));
        Participacion p = new Participacion();
        p.setPracticaId(1L);
        MetricasEvaluacion m = CalculoRubrica.ausencia(p, catalogo);
        assertEquals(BigDecimal.ZERO.setScale(2), m.notaFinal());
        assertEquals(80, m.escalaMaxima());
        assertEquals(3, m.pesoTotal());
        assertEquals(0, m.pesoObtenido());
    }

    @Test
    void sieteDeDiezUsaEscalaDeLaRubrica() {
        CatalogoAcademicoEnMemoria catalogo = new CatalogoAcademicoEnMemoria();
        catalogo.registrar(new PracticaRef(1L, "P", 9L, 5L, null, null, "EN_CURSO", ""));
        for (long i = 1; i <= 10; i++) {
            catalogo.registrar(new PreguntaRef(i, 9L, "P" + i, 1));
            catalogo.registrar(new RespuestaOpcionRef(i * 10, i, "ok", true));
        }
        catalogo.registrar(new RubricaRef(1L, 9L, 5));
        Participacion p = new Participacion();
        p.setPracticaId(1L);
        p.setNumeroIntento(1);
        List<RespuestaSeleccionada> dadas = new ArrayList<>();
        for (long i = 1; i <= 7; i++) {
            RespuestaSeleccionada s = new RespuestaSeleccionada();
            s.setPreguntaId(i);
            s.setRespuestaId(i * 10);
            dadas.add(s);
        }
        RespuestaSeleccionadaRepository repo = mock(RespuestaSeleccionadaRepository.class);
        when(repo.findByParticipacionId(null)).thenReturn(dadas);
        MetricasEvaluacion m = CalculoRubrica.porRespuestas(p, catalogo, repo);
        assertEquals(7, m.pesoObtenido());
        assertEquals(10, m.pesoTotal());
        assertEquals(5, m.escalaMaxima());
        assertEquals(new BigDecimal("3.50"), m.notaFinal());
    }
}
