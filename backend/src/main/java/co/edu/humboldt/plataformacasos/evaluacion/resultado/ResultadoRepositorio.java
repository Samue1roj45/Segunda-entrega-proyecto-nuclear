package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoRepositorio extends JpaRepository<Resultado, Long> {

    Optional<Resultado> findByParticipacionId(Long participacionId);

    @EntityGraph(attributePaths = "participacion")
    List<Resultado> findAllByOrderByIdDesc();

    @Override
    @EntityGraph(attributePaths = "participacion")
    Optional<Resultado> findById(Long id);
}
