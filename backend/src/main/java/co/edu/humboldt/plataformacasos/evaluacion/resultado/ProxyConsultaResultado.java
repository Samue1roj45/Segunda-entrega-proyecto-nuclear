package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.AccesoEvaluacion;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;

// Patrón Proxy de protección: el estudiante sólo ve sus resultados y el docente los de sus sesiones.
/**
 * Protection Proxy RF-14: oculta resultados ajenos. No altera notaFinal ni la nota oficial.
 */
@Service
public class ProxyConsultaResultado implements ConsultaResultado {

    private final FachadaEvaluacion real;
    private final AccesoEvaluacion acceso;

    public ProxyConsultaResultado(FachadaEvaluacion real, AccesoEvaluacion acceso) {
        this.real = real;
        this.acceso = acceso;
    }

    @Override
    public ResultadoRespuesta obtener(Long id) {
        ActorActual actor = acceso.exigirActor();
        acceso.exigirPermisoDeConsulta(actor, Permiso.CONSULTAR_PARTICIPACIONES);
        Resultado resultado = real.exigirResultado(id);
        if (!acceso.visible(actor, resultado.getParticipacion())) {
            throw new RecursoNoEncontradoException("Resultado no encontrado.");
        }
        return real.consultarCerrado(resultado);
    }

    @Override
    public List<ResultadoRespuesta> listar(Long practicaId, Long estudianteId) {
        ActorActual actor = acceso.exigirActor();
        acceso.exigirPermisoDeConsulta(actor, Permiso.CONSULTAR_PARTICIPACIONES);
        return real.listar(practicaId, estudianteId).stream()
                .filter(item -> acceso.visible(actor, item.practicaId(), item.estudianteId()))
                .toList();
    }

    @Override
    public List<NotaOficialRespuesta> listarOficiales(Long practicaId, Long estudianteId) {
        ActorActual actor = acceso.exigirActor();
        acceso.exigirPermisoDeConsulta(actor, Permiso.CONSULTAR_PARTICIPACIONES);
        return real.listarOficiales(practicaId, estudianteId).stream()
                .filter(item -> acceso.visible(actor, item.practicaId(), item.estudianteId()))
                .toList();
    }
}
