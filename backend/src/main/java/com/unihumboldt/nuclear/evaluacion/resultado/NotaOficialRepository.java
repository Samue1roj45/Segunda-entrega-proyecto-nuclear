package com.unihumboldt.nuclear.evaluacion.resultado;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaOficialRepository extends JpaRepository<NotaOficial, Long> {

    @EntityGraph(attributePaths = "participacion")
    Optional<NotaOficial> findByPracticaIdAndEstudianteId(Long practicaId, Long estudianteId);

    @EntityGraph(attributePaths = "participacion")
    List<NotaOficial> findAllByOrderByIdDesc();
}
