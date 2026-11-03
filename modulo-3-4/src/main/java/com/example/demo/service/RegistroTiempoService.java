package com.example.demo.service;

import com.example.demo.dto.RegistroTiempoDto;
import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.repository.ParticipacionRepository;
import com.example.demo.repository.RegistroTiempoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-12
 Para qué sirve: el estudiante ve el registro de inicio y fin de SU participación; el docente ve el de
 todos los participantes de su sesión. La BD es quien registra los instantes; aquí solo se consultan.
*/
@Service
public class RegistroTiempoService {

    private final RegistroTiempoRepository repo;
    private final ParticipacionRepository participaciones;

    public RegistroTiempoService(RegistroTiempoRepository repo, ParticipacionRepository participaciones) {
        this.repo = repo;
        this.participaciones = participaciones;
    }

    @Transactional(readOnly = true)
    public RegistroTiempoDto consultarPropio(Long idEstudiante, Long idParticipacion) {
        if (!participaciones.participacionDeEstudiante(idParticipacion, idEstudiante)) {
            throw new AccesoDenegadoException("La participación no existe o no pertenece al estudiante");
        }
        return repo.porParticipacion(idParticipacion)
                .orElseThrow(() -> new ReglaNegocioException("La participación no existe"));
    }

    @Transactional(readOnly = true)
    public List<RegistroTiempoDto> listarSesion(Long idPractica, Long idDocente) {
        return repo.porSesion(idPractica, idDocente);
    }
}
