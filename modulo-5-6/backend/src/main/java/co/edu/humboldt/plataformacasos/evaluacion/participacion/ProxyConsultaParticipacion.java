package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;

// Patrón Proxy de protección: valida que el actor pueda consultar la participación antes de delegar.
/**
 * Protection Proxy RF-15: el estudiante solo ve sus intentos;
 * el docente solo los de sus prácticas.
 */
@Service
public class ProxyConsultaParticipacion implements ConsultaParticipacion {

    private final FachadaParticipacion real;
    private final AccesoEvaluacion acceso;

    public ProxyConsultaParticipacion(FachadaParticipacion real, AccesoEvaluacion acceso) {
        this.real = real;
        this.acceso = acceso;
    }

    @Override
    public ParticipacionRespuesta obtener(Long id) {
        ActorActual actor = acceso.exigirActor();
        exigirPermiso(actor);
        Participacion participacion = acceso.exigirParticipacion(id);
        acceso.exigirVisible(participacion, actor);
        return real.describir(participacion);
    }

    @Override
    public List<ParticipacionRespuesta> listar(
            Long practicaId,
            Long estudianteId,
            EstadoParticipacion estado,
            Integer numeroIntento,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        ActorActual actor = acceso.exigirActor();
        exigirPermiso(actor);
        return real.listar(practicaId, estudianteId, estado, numeroIntento, fechaDesde, fechaHasta).stream()
                .filter(item -> acceso.visible(actor, item.practicaId(), item.estudianteId()))
                .toList();
    }

    /** RF-02/RF-15: el docente necesita CONSULTAR_PARTICIPACIONES; el estudiante consulta las suyas. */
    private void exigirPermiso(ActorActual actor) {
        if (!actor.esEstudiante()) {
            acceso.exigirPermiso(actor, Permiso.CONSULTAR_PARTICIPACIONES);
        }
    }
}
