package com.unihumboldt.nuclear.evaluacion.resultado;

import java.util.List;

import org.springframework.stereotype.Service;

import com.unihumboldt.nuclear.evaluacion.notificacion.NotificacionFacade;
import com.unihumboldt.nuclear.evaluacion.notificacion.ResumenEnvio;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;

/**
 * Fachada de evaluación. Delega el cálculo, la lectura, la nota oficial y el cierre.
 * Publicar calificaciones no recalcula: lo hace {@link NotificacionFacade}.
 */
@Service
public class EvaluacionFacade {

    private final CalculoNota calculo;
    private final LecturaResultados lectura;
    private final NotaOficialServicio notasOficiales;
    private final CierrePendientes pendientes;
    private final NotificacionFacade notificaciones;

    public EvaluacionFacade(
            CalculoNota calculo,
            LecturaResultados lectura,
            NotaOficialServicio notasOficiales,
            CierrePendientes pendientes,
            NotificacionFacade notificaciones) {
        this.calculo = calculo;
        this.lectura = lectura;
        this.notasOficiales = notasOficiales;
        this.pendientes = pendientes;
        this.notificaciones = notificaciones;
    }

    public Resultado calcular(Participacion participacion) {
        return calculo.calcular(participacion);
    }

    public Resultado exigirResultado(Long id) {
        return lectura.exigir(id);
    }

    public ResultadoRespuesta consultarCerrado(Resultado resultado) {
        return lectura.consultarCerrado(resultado);
    }

    public List<ResultadoRespuesta> listar(Long practicaId, Long estudianteId) {
        return lectura.listar(practicaId, estudianteId);
    }

    public NotaOficialRespuesta seleccionarOficial(SeleccionarNotaOficialRequest request) {
        return notasOficiales.seleccionar(request);
    }

    public List<NotaOficialRespuesta> listarOficiales(Long practicaId, Long estudianteId) {
        return notasOficiales.listar(practicaId, estudianteId);
    }

    public EvaluacionPendientesRespuesta evaluarPendientes(Long practicaId) {
        return pendientes.evaluar(practicaId);
    }

    public ResumenEnvio publicarCalificaciones(Long practicaId) {
        return notificaciones.publicarCalificaciones(practicaId);
    }
}
