package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.humboldt.plataformacasos.evaluacion.notificacion.FachadaNotificaciones;
import co.edu.humboldt.plataformacasos.evaluacion.notificacion.ResumenEnvio;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;

/**
 * Fachada de evaluación. Delega el cálculo, la lectura, la nota oficial y el cierre.
 * Publicar calificaciones no recalcula: lo hace {@link FachadaNotificaciones}.
 */
@Service
public class FachadaEvaluacion {

    private final CalculoNota calculo;
    private final LecturaResultados lectura;
    private final ServicioNotaOficial notasOficiales;
    private final CierrePendientes pendientes;
    private final FachadaNotificaciones notificaciones;

    public FachadaEvaluacion(
            CalculoNota calculo,
            LecturaResultados lectura,
            ServicioNotaOficial notasOficiales,
            CierrePendientes pendientes,
            FachadaNotificaciones notificaciones) {
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
