package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetroalimentacionRepository extends JpaRepository<Retroalimentacion, Long> {

    Optional<Retroalimentacion> findByParticipacionId(Long participacionId);

    @EntityGraph(attributePaths = "participacion")
    List<Retroalimentacion> findAllByOrderByIdDesc();

    @Override
    @EntityGraph(attributePaths = "participacion")
    Optional<Retroalimentacion> findById(Long id);
}
