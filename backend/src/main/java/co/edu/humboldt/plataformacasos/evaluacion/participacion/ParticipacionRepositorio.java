package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface ParticipacionRepositorio extends JpaRepository<Participacion, Long> {

    /** Para iniciar o cerrar intentos: bloquea las filas para que dos peticiones no abran dos intentos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Participacion> findAllByAutorizacionIdOrderByNumeroIntentoAsc(Long autorizacionId);

    /** Solo lectura (consultas del módulo 4): sin bloqueo, válida en transacciones de solo lectura. */
    List<Participacion> findByAutorizacionIdOrderByNumeroIntentoAsc(Long autorizacionId);

    Optional<Participacion> findTopByAutorizacionIdOrderByNumeroIntentoDesc(Long autorizacionId);

    List<Participacion> findAllByOrderByIdDesc();

    List<Participacion> findByPracticaId(Long practicaId);

    List<Participacion> findByEstado(EstadoParticipacion estado);
}
