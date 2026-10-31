package com.unihumboldt.nuclear.evaluacion.resultado;

import java.util.List;

import org.springframework.stereotype.Service;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.RecursoNoEncontradoException;

/**
 * Protection Proxy RF-14: oculta resultados ajenos. No altera notaFinal ni la nota oficial.
 */
@Service
public class ResultadoConsultaProxy implements ConsultaResultado {

    private final EvaluacionFacade real;
    private final AccesoEvaluacion acceso;

    public ResultadoConsultaProxy(EvaluacionFacade real, AccesoEvaluacion acceso) {
        this.real = real;
        this.acceso = acceso;
    }

    @Override
    public ResultadoRespuesta obtener(Long id) {
        ActorActual actor = acceso.exigirActor();
        Resultado resultado = real.exigirResultado(id);
        if (!acceso.visible(actor, resultado.getParticipacion())) {
            throw new RecursoNoEncontradoException("Resultado no encontrado.");
        }
        return real.consultarCerrado(resultado);
    }

    @Override
    public List<ResultadoRespuesta> listar(Long practicaId, Long estudianteId) {
        ActorActual actor = acceso.exigirActor();
        return real.listar(practicaId, estudianteId).stream()
                .filter(item -> acceso.visible(actor, item.practicaId(), item.estudianteId()))
                .toList();
    }

    @Override
    public List<NotaOficialRespuesta> listarOficiales(Long practicaId, Long estudianteId) {
        ActorActual actor = acceso.exigirActor();
        return real.listarOficiales(practicaId, estudianteId).stream()
                .filter(item -> acceso.visible(actor, item.practicaId(), item.estudianteId()))
                .toList();
    }
}
