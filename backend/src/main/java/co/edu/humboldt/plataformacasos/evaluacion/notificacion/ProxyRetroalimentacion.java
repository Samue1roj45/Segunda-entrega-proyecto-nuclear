package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;
import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.AccesoEvaluacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;

// Patrón Proxy de protección: verifica permisos antes de delegar en FachadaRetroalimentacion.
/**
 * Protection Proxy RF-26: solo el docente de la práctica registra;
 * la consulta queda limitada al dueño o a ese docente.
 */
@Service
public class ProxyRetroalimentacion implements GestionRetroalimentacion {

    private final FachadaRetroalimentacion real;
    private final AccesoEvaluacion acceso;

    public ProxyRetroalimentacion(FachadaRetroalimentacion real, AccesoEvaluacion acceso) {
        this.real = real;
        this.acceso = acceso;
    }

    @Override
    public RetroalimentacionRespuesta registrar(RegistrarRetroalimentacionRequest request) {
        ActorActual actor = acceso.exigirActor();
        if (!actor.esDocente() && !actor.esAdmin()) {
            throw new AccesoDenegadoException("Solo el docente puede registrar retroalimentación.");
        }
        acceso.exigirPermiso(actor, Permiso.REGISTRAR_RETROALIMENTACION);
        Participacion participacion = acceso.exigirParticipacion(request.participacionId());
        acceso.exigirPracticaDocente(participacion.getPracticaId(), actor);
        return real.guardar(actor, participacion, request);
    }

    @Override
    public RetroalimentacionRespuesta obtener(Long id) {
        ActorActual actor = acceso.exigirActor();
        Retroalimentacion retro = real.exigir(id);
        if (!acceso.visible(actor, retro.getParticipacion())) {
            throw new RecursoNoEncontradoException("Retroalimentación no encontrada.");
        }
        return real.describir(retro);
    }

    @Override
    public List<RetroalimentacionRespuesta> listar(Long practicaId, Long participacionId) {
        ActorActual actor = acceso.exigirActor();
        return real.listar(practicaId, participacionId).stream()
                .filter(item -> acceso.visible(actor, item.practicaId(), item.estudianteId()))
                .toList();
    }
}
