package co.edu.humboldt.plataformacasos.casos.repositorio;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // ---- Papelera: borrado lógico. Las consultas JPA ya excluyen los eliminados (@SQLRestriction); estas, nativas, no. ----

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE casos_estudio SET fecha_eliminacion = :ahora WHERE id_caso = :id AND fecha_eliminacion IS NULL", nativeQuery = true)
    int marcarEliminado(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE casos_estudio SET fecha_eliminacion = NULL WHERE id_caso = :id AND id_docente = :docente AND fecha_eliminacion IS NOT NULL", nativeQuery = true)
    int restaurar(@Param("id") Long id, @Param("docente") Long docente);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM casos_estudio WHERE id_caso = :id AND id_docente = :docente AND fecha_eliminacion IS NOT NULL", nativeQuery = true)
    int borrarDefinitivamente(@Param("id") Long id, @Param("docente") Long docente);

    @Query(value = "SELECT id_caso, nombre, estado, fecha_eliminacion FROM casos_estudio WHERE id_docente = :docente AND fecha_eliminacion IS NOT NULL ORDER BY fecha_eliminacion DESC, id_caso DESC", nativeQuery = true)
    List<Object[]> papelera(@Param("docente") Long docente);

    @Query(value = "SELECT nombre FROM casos_estudio WHERE id_caso = :id AND id_docente = :docente AND fecha_eliminacion IS NOT NULL", nativeQuery = true)
    Optional<String> nombreEnPapelera(@Param("id") Long id, @Param("docente") Long docente);

    @Query(value = "SELECT COUNT(*) FROM practicas WHERE id_caso = :id", nativeQuery = true)
    long sesionesDelCaso(@Param("id") Long id);

    /** Para los módulos 4 y 5: a qué caso pertenece una opción de respuesta. */
    @Query("select e.caso.id from Respuesta r join r.pregunta p join p.escenario e where r.id = :idRespuesta")
    Optional<Long> casoDeRespuesta(@Param("idRespuesta") Long idRespuesta);
}
