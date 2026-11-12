package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dto.ContextoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.EstadoIngresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ProgresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.TiempoRestanteDto;
import co.edu.humboldt.plataformacasos.sesiones.evento.RespuestaRegistradaEvento;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-10 || RF-11 || RF-22 || RF-23
 Patrón: PROXY (sujeto real) + OBSERVER (publicador del evento) + STRATEGY (vía ServicioIngresoTardio)
 Para qué sirve: lógica real de la participación. NO verifica a quién pertenece la
 participación (eso lo hace el Proxy). Al guardar una respuesta publica un evento.
 RF-23: si el tiempo máximo del intento ya se agotó, la respuesta se rechaza (el intento lo cierra
 la tarea de expiración).
 */
@Service("servicioParticipacionReal")
public class ServicioParticipacionEstudianteImpl implements ServicioParticipacionEstudiante {

    private final ParticipacionEstudianteRepositorio repo;
    private final ApplicationEventPublisher eventos;
    private final ServicioIngresoTardio ingreso;

    public ServicioParticipacionEstudianteImpl(ParticipacionEstudianteRepositorio repo,
                                               ApplicationEventPublisher eventos, ServicioIngresoTardio ingreso) {
        this.repo = repo;
        this.eventos = eventos;
        this.ingreso = ingreso;
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoIngresoDto consultarIngreso(Long idEstudiante, Long idAutorizacion) {
        return ingreso.consultar(idAutorizacion);
    }

    @Override
    @Transactional(readOnly = true)
    public ContextoDto consultarContexto(Long idEstudiante, Long idParticipacion) {
        return repo.consultarContexto(idParticipacion);
    }

    @Override
    @Transactional
    public ProgresoDto responder(Long idEstudiante, Long idParticipacion, Long idPregunta, Long idRespuesta) {
        repo.consultarTiempo(idParticipacion)
                .filter(tiempo -> "EN_CURSO".equals(tiempo.estado()) && tiempo.expirada())
                .ifPresent(tiempo -> {
                    throw new ReglaNegocioException("El tiempo máximo de la participación se agotó");
                });
        ProgresoDto progreso = repo.responder(idParticipacion, idPregunta, idRespuesta);
        eventos.publishEvent(new RespuestaRegistradaEvento(idParticipacion, idPregunta, idRespuesta, progreso));
        return progreso;
    }

    @Override
    @Transactional(readOnly = true)
    public ProgresoDto consultarProgreso(Long idEstudiante, Long idParticipacion) {
        return repo.consultarProgreso(idParticipacion);
    }

    @Override
    @Transactional(readOnly = true)
    public TiempoRestanteDto consultarTiempo(Long idEstudiante, Long idParticipacion) {
        return repo.consultarTiempo(idParticipacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La participación no existe"));
    }
}
