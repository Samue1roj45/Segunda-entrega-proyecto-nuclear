package com.example.demo.service;

import com.example.demo.dto.SesionDto;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.repository.SesionRepository;
import com.example.demo.state.EstadoSesion;
import com.example.demo.state.EstadosSesion;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-18 || RF-19
 Patrón: STATE (contexto)
 Para qué sirve: consulta las sesiones del docente y gobierna su ciclo de vida
 SIN_INICIAR -> EN_CURSO -> FINALIZADO delegando en el objeto de estado actual.
 */
@Service
public class GestionSesionService {

    private static final Set<String> ESTADOS = Set.of("SIN_INICIAR", "EN_CURSO", "FINALIZADO");

    private final SesionRepository repo;

    public GestionSesionService(SesionRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<SesionDto> listar(Long idDocente, String estado) {
        String filtro = null;
        if (estado != null && !estado.isBlank()) {
            filtro = estado.trim().toUpperCase();
            if (!ESTADOS.contains(filtro)) {
                throw new ReglaNegocioException("Estado inválido. Use SIN_INICIAR, EN_CURSO o FINALIZADO");
            }
        }
        return repo.listarPorDocente(idDocente, filtro).stream().map(this::conAcciones).toList();
    }

    @Transactional(readOnly = true)
    public SesionDto obtener(Long idPractica, Long idDocente) {
        return cargar(idPractica, idDocente);
    }

    /** RF-19: inicia la sesión si su estado actual lo permite. */
    @Transactional
    public SesionDto iniciar(Long idPractica, Long idDocente) {
        EstadoSesion estado = EstadosSesion.de(cargar(idPractica, idDocente).estado());
        estado.iniciar(repo, idPractica, idDocente);
        return cargar(idPractica, idDocente);
    }

    /** RF-19: finaliza manualmente la sesión si su estado actual lo permite. */
    @Transactional
    public SesionDto finalizar(Long idPractica, Long idDocente) {
        EstadoSesion estado = EstadosSesion.de(cargar(idPractica, idDocente).estado());
        estado.finalizar(repo, idPractica, idDocente);
        return cargar(idPractica, idDocente);
    }

    private SesionDto cargar(Long idPractica, Long idDocente) {
        return repo.buscar(idPractica, idDocente)
                .map(this::conAcciones)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La sesión no existe o no pertenece al docente"));
    }

    private SesionDto conAcciones(SesionDto s) {
        return s.conAcciones(EstadosSesion.de(s.estado()).accionesPermitidas());
    }
}
