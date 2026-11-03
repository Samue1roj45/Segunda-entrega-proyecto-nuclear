package com.unihumboldt.nuclear.evaluacion.resultado;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.participacion.EstadoParticipacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionadaRepository;

/**
 * RF-13 y RF-24: elige la estrategia y persiste la nota una sola vez.
 */
@Service
public class CalculoNota {

    private final ResultadoRepository resultados;
    private final RespuestaSeleccionadaRepository respuestas;
    private final CatalogoAcademico catalogo;
    private final Clock clock;

    public CalculoNota(
            ResultadoRepository resultados,
            RespuestaSeleccionadaRepository respuestas,
            CatalogoAcademico catalogo,
            Clock clock) {
        this.resultados = resultados;
        this.respuestas = respuestas;
        this.catalogo = catalogo;
        this.clock = clock;
    }

    @Transactional
    public Resultado calcular(Participacion participacion) {
        return resultados.findByParticipacionId(participacion.getId()).orElseGet(() -> persistir(participacion));
    }

    private Resultado persistir(Participacion participacion) {
        MetricasEvaluacion metricas = estrategiaPara(participacion).calcular(participacion);
        Resultado resultado = new Resultado();
        resultado.setParticipacion(participacion);
        resultado.setCorrectas(metricas.correctas());
        resultado.setIncorrectas(metricas.incorrectas());
        resultado.setNoRespondidas(metricas.noRespondidas());
        resultado.setPesoObtenido(metricas.pesoObtenido());
        resultado.setPesoTotal(metricas.pesoTotal());
        resultado.setEscalaMaxima(metricas.escalaMaxima());
        resultado.setNotaFinal(metricas.notaFinal());
        resultado.setFechaCalculo(OffsetDateTime.now(clock));
        return resultados.save(resultado);
    }

    private EstrategiaEvaluacion estrategiaPara(Participacion participacion) {
        if (participacion.getEstado() == EstadoParticipacion.FINALIZADA) {
            return new EvaluacionCompletaStrategy(catalogo, respuestas);
        }
        if (participacion.getEstado() == EstadoParticipacion.INCOMPLETA) {
            return new EvaluacionIncompletaStrategy(catalogo, respuestas);
        }
        return new EvaluacionAusenciaStrategy(catalogo);
    }
}
