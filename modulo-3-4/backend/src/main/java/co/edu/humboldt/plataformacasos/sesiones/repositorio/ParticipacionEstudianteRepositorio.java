package co.edu.humboldt.plataformacasos.sesiones.repositorio;

import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoParticipacionDTO;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.IntentoParticipacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RegistroParticipaciones;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Autorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosTiempoSesion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import co.edu.humboldt.plataformacasos.sesiones.dto.ContextoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ProgresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.TiempoRestanteDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-10 || RF-11 || RF-22 || RF-23
 Patrón: GATEWAY (adaptador)
 Para qué sirve: punto único por el que este módulo lee y registra la participación del estudiante.
 El intento y sus respuestas pertenecen al módulo 5 (RF-12) y se usan por su puerto
 RegistroParticipaciones; el contenido del caso pertenece al módulo 2 y se lee SIN la clave
 (ContenidoCasoInterno.contenidoSinClaves); la sesión y la autorización son de este módulo.
 Así los servicios no saben de qué módulo viene cada dato.
*/
@Repository
public class ParticipacionEstudianteRepositorio {

    private final RegistroParticipaciones registro;
    private final ContenidoCasoInterno casos;
    private final SesionRepositorio sesiones;
    private final AutorizacionRepositorio autorizaciones;
    private final Clock reloj;

    public ParticipacionEstudianteRepositorio(RegistroParticipaciones registro, ContenidoCasoInterno casos,
                                              SesionRepositorio sesiones, AutorizacionRepositorio autorizaciones,
                                              Clock reloj) {
        this.registro = registro;
        this.casos = casos;
        this.sesiones = sesiones;
        this.autorizaciones = autorizaciones;
        this.reloj = reloj;
    }

    // ---------- Responder / progreso (RF-10, RF-11) ----------

    /** Guarda o cambia la respuesta (una por pregunta) y devuelve el progreso actualizado. */
    public ProgresoDto responder(Long idParticipacion, Long idPregunta, Long idRespuesta) {
        registro.registrarRespuesta(idParticipacion, idPregunta, idRespuesta);
        return consultarProgreso(idParticipacion);
    }

    public ProgresoDto consultarProgreso(Long idParticipacion) {
        IntentoParticipacion intento = intento(idParticipacion);
        Set<Long> preguntas = idsPreguntas(contenido(intento));
        Map<Long, Long> elegidas = registro.respuestasSeleccionadas(idParticipacion);
        int respondidas = (int) elegidas.keySet().stream().filter(preguntas::contains).count();
        int total = preguntas.size();
        BigDecimal porcentaje = total == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(respondidas * 100L).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        return new ProgresoDto(idParticipacion, total, respondidas, porcentaje);
    }

    // ---------- RF-22: ingreso tardío ----------

    public boolean tieneParticipacionEnCurso(Long idAutorizacion) {
        return registro.intentosDeAutorizacion(idAutorizacion).stream().anyMatch(IntentoParticipacion::enCurso);
    }

    public Optional<DatosTiempoSesion> datosTiempo(Long idAutorizacion) {
        return autorizaciones.buscar(idAutorizacion).flatMap(a -> sesiones.buscar(a.idPractica()).map(p ->
                new DatosTiempoSesion(p.inicioEfectivo(), p.tiempoMaximoMinutos(), p.porcentajeLimiteIngreso(),
                        p.estado(), a.estado(), LocalDateTime.now(reloj))));
    }

    // ---------- RF-23: tiempo máximo ----------

    /** Tiempo restante del intento: corre desde que el estudiante inicia hasta inicio + tiempo máximo. */
    public Optional<TiempoRestanteDto> consultarTiempo(Long idParticipacion) {
        return registro.intento(idParticipacion).flatMap(intento -> sesiones.buscar(intento.practicaId()).map(p -> {
            LocalDateTime ahora = LocalDateTime.now(reloj);
            LocalDateTime inicio = local(intento.inicio());
            boolean enCurso = intento.enCurso();
            LocalDateTime limite = inicio == null ? null : inicio.plusMinutes(p.tiempoMaximoMinutos());
            long segundos = (limite == null || !enCurso) ? 0
                    : Math.max(0, Duration.between(ahora, limite).getSeconds());
            boolean expirada = !enCurso || (limite != null && ahora.isAfter(limite));
            return new TiempoRestanteDto(idParticipacion, intento.estado().name(), inicio, limite, segundos, expirada);
        }));
    }

    /** RF-23: cierra los intentos EN_CURSO cuyo tiempo máximo ya se agotó. Devuelve cuántos cerró. */
    public int expirarVencidas() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int cerrados = 0;
        for (IntentoParticipacion intento : registro.intentosEnCurso()) {
            Optional<Practica> practica = sesiones.buscar(intento.practicaId());
            LocalDateTime inicio = local(intento.inicio());
            if (practica.isPresent() && inicio != null
                    && ahora.isAfter(inicio.plusMinutes(practica.get().tiempoMaximoMinutos()))) {
                registro.cerrarPorTiempo(intento.id());
                cerrados++;
            }
        }
        return cerrados;
    }

    // ---------- Contexto del caso (RF-10) ----------

    public ContextoDto consultarContexto(Long idParticipacion) {
        IntentoParticipacion intento = intento(idParticipacion);
        ContenidoParticipacionDTO caso = contenido(intento);
        Map<Long, Long> elegidas = registro.respuestasSeleccionadas(idParticipacion);
        List<ContextoDto.Escenario> escenarios = caso.escenarios().stream()
                .map(e -> new ContextoDto.Escenario(e.id(), e.orden(), e.nombre(), e.contexto(),
                        e.preguntas().stream()
                                .map(q -> new ContextoDto.Pregunta(q.id(), q.orden(), q.enunciado(), elegidas.get(q.id()),
                                        q.opciones().stream()
                                                .map(o -> new ContextoDto.Opcion(o.id(), o.codigo(), o.texto()))
                                                .toList()))
                                .toList()))
                .toList();
        return new ContextoDto(caso.nombre(), caso.contextoHistoria(), caso.desarrolloSituacional(), escenarios);
    }

    // ---------- Datos que usan el Proxy y las herramientas (RF-28) ----------

    public boolean participacionDeEstudiante(Long idParticipacion, Long idEstudiante) {
        return registro.intento(idParticipacion).map(i -> i.estudianteId().equals(idEstudiante)).orElse(false);
    }

    public boolean autorizacionDeEstudiante(Long idAutorizacion, Long idEstudiante) {
        return autorizaciones.buscar(idAutorizacion).map(Autorizacion::idEstudiante)
                .map(idEstudiante::equals).orElse(false);
    }

    public String estadoParticipacion(Long idParticipacion) {
        return intento(idParticipacion).estado().name();
    }

    public Long idCasoDeParticipacion(Long idParticipacion) {
        return practicaDe(intento(idParticipacion)).idCaso();
    }

    // ---------- apoyo ----------

    private IntentoParticipacion intento(Long idParticipacion) {
        return registro.intento(idParticipacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La participación no existe"));
    }

    private Practica practicaDe(IntentoParticipacion intento) {
        return sesiones.buscar(intento.practicaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La sesión de la participación no existe"));
    }

    private ContenidoParticipacionDTO contenido(IntentoParticipacion intento) {
        return casos.contenidoSinClaves(practicaDe(intento).idCaso());
    }

    private static Set<Long> idsPreguntas(ContenidoParticipacionDTO caso) {
        return caso.escenarios().stream().flatMap(e -> e.preguntas().stream())
                .map(ContenidoParticipacionDTO.PreguntaVisible::id).collect(Collectors.toSet());
    }

    private LocalDateTime local(OffsetDateTime instante) {
        return instante == null ? null : instante.atZoneSameInstant(reloj.getZone()).toLocalDateTime();
    }
}
