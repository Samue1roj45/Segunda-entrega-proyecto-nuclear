package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaSeleccionadaRepositorio extends JpaRepository<RespuestaSeleccionada, Long> {

    List<RespuestaSeleccionada> findByParticipacionId(Long participacionId);

    Optional<RespuestaSeleccionada> findByParticipacionIdAndPreguntaId(Long participacionId, Long preguntaId);
}
