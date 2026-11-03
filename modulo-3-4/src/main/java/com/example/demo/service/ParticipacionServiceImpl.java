package com.example.demo.service;

import com.example.demo.dto.ContextoDto;
import com.example.demo.dto.EstadoIngresoDto;
import com.example.demo.dto.FinalizacionDto;
import com.example.demo.dto.ProgresoDto;
import com.example.demo.dto.TiempoRestanteDto;
import com.example.demo.event.RespuestaRegistradaEvent;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.repository.ParticipacionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-10 || RF-11 || RF-22 || RF-23
 Patrón: PROXY (sujeto real) + OBSERVER (publicador del evento) + STRATEGY (vía IngresoTardioService)
 Para qué sirve: lógica real de la participación. NO verifica a quién pertenece la
 participación (eso lo hace el Proxy). Al guardar una respuesta publica un evento.
 */
@Service("participacionServiceReal")
public class ParticipacionServiceImpl implements ParticipacionService {

    private final ParticipacionRepository repo;
    private final ApplicationEventPublisher eventos;
    private final IngresoTardioService ingreso;

    public ParticipacionServiceImpl(ParticipacionRepository repo, ApplicationEventPublisher eventos,
                                    IngresoTardioService ingreso) {
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
    @Transactional
    public Long iniciar(Long idEstudiante, Long idAutorizacion) {
        // RF-22: retomar un intento EN_CURSO siempre se permite; EMPEZAR uno nuevo respeta el límite
        if (!repo.tieneParticipacionEnCurso(idAutorizacion)) {
            ingreso.exigirPermitido(idAutorizacion);
        }
        return repo.iniciar(idAutorizacion);
    }

    @Override
    @Transactional(readOnly = true)
    public ContextoDto consultarContexto(Long idEstudiante, Long idParticipacion) {
        return repo.consultarContexto(idParticipacion);
    }

    @Override
    @Transactional
    public ProgresoDto responder(Long idEstudiante, Long idParticipacion, Long idPregunta, Long idRespuesta) {
        ProgresoDto progreso = repo.responder(idParticipacion, idPregunta, idRespuesta);
        eventos.publishEvent(new RespuestaRegistradaEvent(idParticipacion, idPregunta, idRespuesta, progreso));
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
                .orElseThrow(() -> new ReglaNegocioException("La participación no existe"));
    }

    @Override
    @Transactional
    public FinalizacionDto finalizar(Long idEstudiante, Long idParticipacion) {
        return repo.finalizar(idParticipacion);
    }
}
