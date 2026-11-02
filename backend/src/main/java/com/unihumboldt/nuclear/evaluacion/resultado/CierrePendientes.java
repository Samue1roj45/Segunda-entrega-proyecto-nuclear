package com.unihumboldt.nuclear.evaluacion.resultado;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.AutorizacionRef;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.EstadoParticipacion;
import com.unihumboldt.nuclear.evaluacion.participacion.MaquinaEstadoParticipacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.ParticipacionRepository;

/**
 * RF-24: cierra intentos en curso como incompletos y registra ausencias en NO_INICIADA.
 */
@Service
public class CierrePendientes {

    private final ParticipacionRepository participaciones;
    private final ResultadoRepository resultados;
    private final CatalogoAcademico catalogo;
    private final AccesoEvaluacion acceso;
    private final CalculoNota calculo;
    private final Clock clock;

    public CierrePendientes(
            ParticipacionRepository participaciones,
            ResultadoRepository resultados,
            CatalogoAcademico catalogo,
            AccesoEvaluacion acceso,
            CalculoNota calculo,
            Clock clock) {
        this.participaciones = participaciones;
        this.resultados = resultados;
        this.catalogo = catalogo;
        this.acceso = acceso;
        this.calculo = calculo;
        this.clock = clock;
    }

    @Transactional
    public EvaluacionPendientesRespuesta evaluar(Long practicaId) {
        PracticaRef practica = acceso.exigirPracticaDocente(practicaId, acceso.exigirActor());
        int incompletas = 0;
        int ausencias = 0;
        int omitidas = 0;
        for (AutorizacionRef autorizacion : catalogo.autorizacionesDePractica(practica.id())) {
            List<Participacion> existentes = participaciones.findAllByAutorizacionIdOrderByNumeroIntentoAsc(autorizacion.id());
            List<Participacion> enCurso = existentes.stream()
                    .filter(p -> p.getEstado() == EstadoParticipacion.EN_CURSO)
                    .toList();
            if (!enCurso.isEmpty()) {
                for (Participacion part : enCurso) {
                    MaquinaEstadoParticipacion.marcarIncompleta(part, OffsetDateTime.now(clock));
                    participaciones.save(part);
                    calculo.calcular(part);
                    incompletas++;
                }
                continue;
            }
            if (existentes.stream().anyMatch(Participacion::esTerminal)) {
                omitidas++;
                continue;
            }
            Participacion noIniciada = existentes.stream()
                    .filter(p -> p.getEstado() == EstadoParticipacion.NO_INICIADA)
                    .findFirst()
                    .orElse(null);
            if (noIniciada == null) {
                noIniciada = new Participacion();
                noIniciada.setPracticaId(practica.id());
                noIniciada.setEstudianteId(autorizacion.estudianteId());
                noIniciada.setAutorizacionId(autorizacion.id());
                noIniciada.setNumeroIntento(1);
                noIniciada = participaciones.save(noIniciada);
            }
            if (resultados.findByParticipacionId(noIniciada.getId()).isPresent()) {
                omitidas++;
                continue;
            }
            calculo.calcular(noIniciada);
            ausencias++;
        }
        return new EvaluacionPendientesRespuesta(incompletas, ausencias, omitidas);
    }
}
