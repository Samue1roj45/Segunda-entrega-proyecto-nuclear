package com.example.demo.service;

import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.repository.HerramientasRepository;
import com.example.demo.repository.ParticipacionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-28
 Para qué sirve: el estudiante consulta las herramientas profesionales del caso SOLO mientras su
 participación está en curso y solo si la participación es suya (mismo control de acceso del Proxy).
 */
@Service
public class HerramientasService {

    private final HerramientasRepository repo;
    private final ParticipacionRepository participaciones;
    private final String rutaHerramientas;

    public HerramientasService(HerramientasRepository repo, ParticipacionRepository participaciones,
                               @Value("${soma.dsl.ruta-herramientas:$.herramientas}") String rutaHerramientas) {
        this.repo = repo;
        this.participaciones = participaciones;
        this.rutaHerramientas = rutaHerramientas;
    }

    @Transactional(readOnly = true)
    public String consultar(Long idEstudiante, Long idParticipacion) {
        if (!participaciones.participacionDeEstudiante(idParticipacion, idEstudiante)) {
            throw new AccesoDenegadoException("La participación no existe o no pertenece al estudiante");
        }
        if (!"EN_CURSO".equals(repo.estadoParticipacion(idParticipacion))) {
            throw new ReglaNegocioException("Las herramientas solo están disponibles mientras la participación está en curso");
        }
        // Las herramientas son las que define cada caso (sin catálogo propio): si el caso no
        // definió ninguna, el estudiante recibe una lista vacía y puede participar igual.
        return repo.herramientasDelCaso(idParticipacion, rutaHerramientas).orElse("[]");
    }
}
