package com.unihumboldt.nuclear.evaluacion.participacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface ParticipacionRepository extends JpaRepository<Participacion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Participacion> findAllByAutorizacionIdOrderByNumeroIntentoAsc(Long autorizacionId);

    Optional<Participacion> findTopByAutorizacionIdOrderByNumeroIntentoDesc(Long autorizacionId);

    List<Participacion> findAllByOrderByIdDesc();

    List<Participacion> findByPracticaId(Long practicaId);
}
