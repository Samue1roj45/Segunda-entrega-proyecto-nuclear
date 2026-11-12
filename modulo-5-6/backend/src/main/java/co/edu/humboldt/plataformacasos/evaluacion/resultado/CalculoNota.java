package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.EstadoParticipacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RespuestaSeleccionadaRepositorio;

/**
 * RF-13 y RF-24: elige la estrategia y persiste la nota una sola vez.
 */
@Service
public class CalculoNota {

    private final ResultadoRepositorio resultados;
    private final RespuestaSeleccionadaRepositorio respuestas;
    private final CatalogoAcademico catalogo;
    private final Clock clock;

    public CalculoNota(
            ResultadoRepositorio resultados,
            RespuestaSeleccionadaRepositorio respuestas,
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
            return new EstrategiaEvaluacionCompleta(catalogo, respuestas);
        }
        if (participacion.getEstado() == EstadoParticipacion.INCOMPLETA) {
            return new EstrategiaEvaluacionIncompleta(catalogo, respuestas);
        }
        return new EstrategiaEvaluacionAusencia(catalogo);
    }
}
