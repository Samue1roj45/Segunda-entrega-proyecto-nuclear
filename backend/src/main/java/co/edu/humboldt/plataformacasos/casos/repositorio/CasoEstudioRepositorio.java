package co.edu.humboldt.plataformacasos.casos.repositorio;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CasoEstudioRepositorio extends JpaRepository<CasoEstudio, Long> {

    @Query("""
            select new co.edu.humboldt.plataformacasos.casos.repositorio.CasoResumen(
                c.id, c.nombre, c.tiempoEstimadoMinutos, c.estado, c.fechaActualizacion, c.fechaPublicacion)
            from CasoEstudio c
            where c.docenteId = :docenteId and (:estado is null or c.estado = :estado)
            order by c.fechaActualizacion desc, c.id desc
            """)
    List<CasoResumen> listarDelDocente(@Param("docenteId") Long docenteId, @Param("estado") EstadoCaso estado);

    /**
     * Bloquea la raíz del agregado durante la transacción: dos modificaciones del mismo caso no
     * se cruzan, y una edición no puede colarse entre la validación y la publicación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CasoEstudio c where c.id = :id")
    Optional<CasoEstudio> buscarParaModificar(@Param("id") Long id);

    /** Para otros módulos: un caso solo existe para ellos después de publicarse. */
    Optional<CasoEstudio> findByIdAndEstadoNot(Long id, EstadoCaso estadoExcluido);

    @Query("""
            select new co.edu.humboldt.plataformacasos.casos.repositorio.CasoProgramable(
                c.id, c.nombre, c.tiempoEstimadoMinutos, c.docenteId, c.estado, c.fechaPublicacion)
            from CasoEstudio c
            where c.estado = :estado
            order by c.fechaPublicacion desc, c.id desc
            """)
    List<CasoProgramable> listarPorEstado(@Param("estado") EstadoCaso estado);
}
