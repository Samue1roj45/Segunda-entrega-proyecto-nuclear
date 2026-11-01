package com.example.demo.service;

import com.example.demo.dto.ConvocadoDto;
import com.example.demo.dto.SesionCreadaResponse;
import com.example.demo.model.SesionProgramada;
import com.example.demo.repository.SesionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-04 || RF-18
 Para qué sirve: casos de uso de programación. Agendar + asociar estudiantes ocurre
 en UNA transacción: si un estudiante es inválido, no queda la sesión a medias.
 */
@Service
public class SesionService {

    private final SesionRepository repo;

    public SesionService(SesionRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public SesionCreadaResponse agendarConEstudiantes(SesionProgramada s) {
        Long idPractica = repo.agendar(s);
        for (Long idEstudiante : s.idsEstudiantes()) {
            repo.agregarEstudiante(idPractica, s.idDocente(), idEstudiante);   // RF-04
        }
        return new SesionCreadaResponse(idPractica, repo.listarConvocados(idPractica, s.idDocente()));
    }

    @Transactional
    public Long agregarEstudiante(Long idPractica, Long idDocente, Long idEstudiante) {
        return repo.agregarEstudiante(idPractica, idDocente, idEstudiante);
    }

    @Transactional
    public void retirarEstudiante(Long idPractica, Long idDocente, Long idEstudiante) {
        repo.retirarEstudiante(idPractica, idDocente, idEstudiante);
    }

    @Transactional(readOnly = true)
    public List<ConvocadoDto> listarConvocados(Long idPractica, Long idDocente) {
        return repo.listarConvocados(idPractica, idDocente);
    }
}
