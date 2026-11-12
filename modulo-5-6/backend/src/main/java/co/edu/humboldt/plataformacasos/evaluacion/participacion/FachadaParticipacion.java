package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.AutorizacionRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.EstudianteRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PoliticaInicioIntento;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;
import co.edu.humboldt.plataformacasos.evaluacion.resultado.CalculoNota;
import co.edu.humboldt.plataformacasos.evaluacion.resultado.ResultadoRepositorio;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Patrón Facade: orquesta iniciar, responder y finalizar un intento (RF-12) ocultando estados, repositorios y políticas.
/**
 * RF-12 (Facade): registra el inicio y el fin de cada intento. Antes de abrir un intento nuevo
 * consulta la regla de ingreso del módulo 4 (RF-22) y el número de intentos permitidos de la
 * autorización (RF-25, módulo 3). Las respuestas (RF-10, RF-11) las registra el módulo 4 por el
 * puerto {@link RegistroParticipaciones}.
 */
@Service
public class FachadaParticipacion {

    private final ParticipacionRepositorio repositorio;
    private final RespuestaSeleccionadaRepositorio respuestas;
    private final ResultadoRepositorio resultados;
    private final CatalogoAcademico catalogo;
    private final AccesoEvaluacion acceso;
    private final CalculoNota evaluacion;
    private final PoliticaInicioIntento politicaInicio;
    private final Clock clock;

    public FachadaParticipacion(
            ParticipacionRepositorio repositorio,
            RespuestaSeleccionadaRepositorio respuestas,
            ResultadoRepositorio resultados,
            CatalogoAcademico catalogo,
            AccesoEvaluacion acceso,
            CalculoNota evaluacion,
            PoliticaInicioIntento politicaInicio,
            Clock clock) {
        this.repositorio = repositorio;
        this.respuestas = respuestas;
        this.resultados = resultados;
        this.catalogo = catalogo;
        this.acceso = acceso;
        this.evaluacion = evaluacion;
        this.politicaInicio = politicaInicio;
        this.clock = clock;
    }

    @Transactional
    public ParticipacionRespuesta iniciar(IniciarParticipacionRequest request) {
        ActorActual actor = acceso.exigirActor();
        if (!actor.esEstudiante() || !actor.tienePermiso(Permiso.PARTICIPAR_EN_CASOS)) {
            throw new AccesoDenegadoException("Solo estudiantes pueden iniciar participaciones.");
        }
        if (request == null || request.autorizacionId() == null) {
            throw new ReglaNegocioException("Se requiere autorizacionId.");
        }

        AutorizacionRef autorizacion = catalogo.buscarAutorizacion(request.autorizacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Autorización no encontrada."));
        if (!Objects.equals(autorizacion.estudianteId(), actor.estudianteId())) {
            throw new AccesoDenegadoException("La autorización no corresponde al estudiante autenticado.");
        }

        PracticaRef practica = catalogo.buscarPractica(autorizacion.practicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica no encontrada."));

        OffsetDateTime ahora = OffsetDateTime.now(clock);
        if (!autorizacion.activa()) {
            throw new ReglaNegocioException("La autorización del estudiante fue retirada.");
        }
        if (practica.finalizada() || ahora.isAfter(practica.fechaFin())) {
            throw new ReglaNegocioException("La práctica ya finalizó.");
        }

        List<Participacion> existentes = repositorio.findAllByAutorizacionIdOrderByNumeroIntentoAsc(autorizacion.id());
        Participacion enCurso = existentes.stream()
                .filter(p -> p.getEstado() == EstadoParticipacion.EN_CURSO)
                .findFirst()
                .orElse(null);
        if (enCurso != null) {
            return toRespuesta(enCurso, practica);   // retomar un intento en curso siempre se permite
        }

        // RF-22 (módulo 4): sesión en curso y dentro del límite de ingreso tardío.
        politicaInicio.exigirInicioPermitido(autorizacion.id());

        Participacion noIniciadaSinNota = existentes.stream()
                .filter(p -> p.getEstado() == EstadoParticipacion.NO_INICIADA)
                .filter(p -> !intentoConNota(p))
                .findFirst()
                .orElse(null);
        if (noIniciadaSinNota != null) {
            return toRespuesta(repositorio.save(MaquinaEstadoParticipacion.iniciar(noIniciadaSinNota, ahora)), practica);
        }

        // RF-25: 1 intento + repeticiones autorizadas; una ausencia (NO_INICIADA) no cuenta como intento.
        long usados = existentes.stream().filter(p -> p.getEstado() != EstadoParticipacion.NO_INICIADA).count();
        if (usados >= autorizacion.intentosPermitidos()) {
            throw new ReglaNegocioException(autorizacion.intentosPermitidos() == 1
                    ? "Ya usaste tu intento. Solo el docente creador del caso puede autorizar una repetición."
                    : "Ya usaste los " + autorizacion.intentosPermitidos() + " intentos permitidos.");
        }

        int siguiente = existentes.stream().mapToInt(Participacion::getNumeroIntento).max().orElse(0) + 1;
        Participacion nueva = new Participacion();
        nueva.setPracticaId(practica.id());
        nueva.setEstudianteId(autorizacion.estudianteId());
        nueva.setAutorizacionId(autorizacion.id());
        nueva.setNumeroIntento(siguiente);
        MaquinaEstadoParticipacion.iniciar(nueva, ahora);
        return toRespuesta(repositorio.save(nueva), practica);
    }

    @Transactional
    public ParticipacionRespuesta finalizar(Long id) {
        ActorActual actor = acceso.exigirActor();
        Participacion participacion = acceso.exigirDueñoEstudiante(id, actor);
        OffsetDateTime ahora = OffsetDateTime.now(clock);
        MaquinaEstadoParticipacion.finalizar(participacion, ahora);
        participacion = repositorio.save(participacion);
        evaluacion.calcular(participacion);
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId()).orElse(null);
        return toRespuesta(participacion, practica);
    }

    @Transactional(readOnly = true)
    public ParticipacionRespuesta describir(Participacion participacion) {
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId()).orElse(null);
        return toRespuesta(participacion, practica);
    }

    @Transactional(readOnly = true)
    public List<ParticipacionRespuesta> listar(
            Long practicaId,
            Long estudianteId,
            EstadoParticipacion estado,
            Integer numeroIntento,
            LocalDate fechaDesde,
            LocalDate fechaHasta) {
        return repositorio.findAllByOrderByIdDesc().stream()
                .filter(p -> practicaId == null || Objects.equals(p.getPracticaId(), practicaId))
                .filter(p -> estudianteId == null || Objects.equals(p.getEstudianteId(), estudianteId))
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> numeroIntento == null || p.getNumeroIntento() == numeroIntento)
                .filter(p -> fechaDesde == null || (p.getInicio() != null && !p.getInicio().toLocalDate().isBefore(fechaDesde)))
                .filter(p -> fechaHasta == null || (p.getInicio() != null && !p.getInicio().toLocalDate().isAfter(fechaHasta)))
                .map(p -> toRespuesta(p, catalogo.buscarPractica(p.getPracticaId()).orElse(null)))
                .toList();
    }

    private boolean intentoConNota(Participacion participacion) {
        return participacion.getId() != null
                && resultados.findByParticipacionId(participacion.getId()).isPresent();
    }

    private ParticipacionRespuesta toRespuesta(Participacion participacion, PracticaRef practica) {
        EstudianteRef estudiante = catalogo.buscarEstudiante(participacion.getEstudianteId()).orElse(null);
        return new ParticipacionRespuesta(
                participacion.getId(),
                participacion.getPracticaId(),
                practica != null ? practica.nombre() : null,
                participacion.getEstudianteId(),
                estudiante != null ? estudiante.correo() : null,
                estudiante != null ? estudiante.nombreCompleto() : null,
                participacion.getAutorizacionId(),
                participacion.getNumeroIntento(),
                participacion.getInicio(),
                participacion.getFin(),
                participacion.getTiempoUsadoSeg(),
                participacion.getEstado());
    }
}
