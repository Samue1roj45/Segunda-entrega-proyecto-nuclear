package co.edu.humboldt.plataformacasos.integracion;

import co.edu.humboldt.plataformacasos.casos.integracion.ClaveEvaluacionDTO;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CriterioDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RubricaDTO;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.AutorizacionRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.EstudianteRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PreguntaRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.RespuestaOpcionRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.RubricaRef;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Autorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import co.edu.humboldt.plataformacasos.sesiones.integracion.ConsultaSesiones;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Patrón Adapter (Ports & Adapters): adapta los datos de sesiones de M3 al puerto CatalogoAcademico de M5.
/**
 * Catálogo académico real de los módulos 5 y 6:
 * <ul>
 *   <li>sesiones y autorizaciones: módulo 3 ({@link ConsultaSesiones});</li>
 *   <li>estudiantes: módulo 1 (usuarios con rol ESTUDIANTE);</li>
 *   <li>preguntas, opciones correctas y rúbrica: módulo 2 ({@link ContenidoCasoInterno#claveDeEvaluacion}).</li>
 * </ul>
 * El módulo 2 no define pesos por pregunta, así que todas pesan 1. La escala de la nota es el mayor
 * rango máximo de los criterios de la rúbrica (por ejemplo 5.0); si no hay rangos, 100.
 */
@Component
@Transactional(readOnly = true)
public class CatalogoAcademicoIntegrado implements CatalogoAcademico {

    private static final int PESO_PREGUNTA = 1;
    private static final int ESCALA_POR_DEFECTO = 100;

    private final ConsultaSesiones sesiones;
    private final ContenidoCasoInterno casos;
    private final UsuarioRepositorio usuarios;
    private final Clock reloj;

    public CatalogoAcademicoIntegrado(ConsultaSesiones sesiones, ContenidoCasoInterno casos,
                                      UsuarioRepositorio usuarios, Clock reloj) {
        this.sesiones = sesiones;
        this.casos = casos;
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    @Override
    public Optional<PracticaRef> buscarPractica(Long id) {
        return sesiones.sesion(id).map(this::practica);
    }

    @Override
    public Optional<AutorizacionRef> buscarAutorizacion(Long id) {
        return sesiones.autorizacion(id).map(this::autorizacion);
    }

    @Override
    public Optional<EstudianteRef> buscarEstudiante(Long id) {
        return usuarios.findById(id)
                .filter(u -> u.getRol() == Rol.ESTUDIANTE)
                .map(u -> new EstudianteRef(u.getId(), u.getCorreoInstitucional(), u.getNombreCompleto(), u.getId()));
    }

    @Override
    public List<AutorizacionRef> autorizacionesDePractica(Long practicaId) {
        return sesiones.autorizacionesActivas(practicaId).stream().map(this::autorizacion).toList();
    }

    @Override
    public List<PreguntaRef> preguntasDelCaso(Long casoId) {
        return clave(casoId).map(c -> c.preguntas().stream()
                        .map(p -> new PreguntaRef(p.id(), casoId, p.enunciado(), PESO_PREGUNTA))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public Optional<RespuestaOpcionRef> buscarRespuesta(Long id) {
        return casos.casoDeRespuesta(id).flatMap(this::clave).flatMap(c -> c.preguntas().stream()
                .flatMap(p -> p.opciones().stream()
                        .filter(o -> Objects.equals(o.id(), id))
                        .map(o -> new RespuestaOpcionRef(o.id(), p.id(), o.texto(), o.esCorrecta())))
                .findFirst());
    }

    @Override
    public Optional<RubricaRef> buscarRubricaPorCaso(Long casoId) {
        return clave(casoId).map(ClaveEvaluacionDTO::rubrica)
                .map(r -> new RubricaRef(r.id(), casoId, escala(r)));
    }

    private PracticaRef practica(Practica p) {
        return new PracticaRef(p.id(), nombreCaso(p.idCaso()), p.idCaso(), p.idDocente(), conZona(p.fechaInicio()),
                conZona(p.fechaFin()), p.estado(), p.mensajePersonalizado(), p.tiempoMaximoMinutos());
    }

    private AutorizacionRef autorizacion(Autorizacion a) {
        return new AutorizacionRef(a.id(), a.idPractica(), a.idEstudiante(), a.activa(),
                sesiones.intentosPermitidos(a.id()));
    }

    private String nombreCaso(Long casoId) {
        try {
            return casos.resumen(casoId).nombre();
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
    }

    /** Un caso borrado o en BORRADOR no existe para otros módulos (regla del módulo 2). */
    private Optional<ClaveEvaluacionDTO> clave(Long casoId) {
        try {
            return Optional.of(casos.claveDeEvaluacion(casoId));
        } catch (RecursoNoEncontradoException e) {
            return Optional.empty();
        }
    }

    private static int escala(RubricaDTO rubrica) {
        return rubrica.criteriosEvaluacion().stream()
                .map(CriterioDTO::rango)
                .filter(Objects::nonNull)
                .map(r -> r.max())
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .map(max -> max.setScale(0, RoundingMode.HALF_UP).intValue())
                .filter(max -> max > 0)
                .orElse(ESCALA_POR_DEFECTO);
    }

    private OffsetDateTime conZona(LocalDateTime fecha) {
        return fecha == null ? null : fecha.atZone(reloj.getZone()).toOffsetDateTime();
    }
}
