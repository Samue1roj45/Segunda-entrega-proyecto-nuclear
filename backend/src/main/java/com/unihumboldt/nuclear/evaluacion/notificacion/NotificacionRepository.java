package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    Optional<Notificacion> findByClaveIdempotencia(String claveIdempotencia);

    List<Notificacion> findByEstudianteIdOrderByIdDesc(Long estudianteId);

    List<Notificacion> findByPracticaIdOrderByIdDesc(Long practicaId);

    List<Notificacion> findAllByOrderByIdDesc();
}
