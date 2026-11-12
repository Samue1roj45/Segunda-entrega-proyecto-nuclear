package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.AutorizacionRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.EstudianteRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.AccesoEvaluacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.evaluacion.resultado.Resultado;
import co.edu.humboldt.plataformacasos.evaluacion.resultado.ResultadoRepositorio;

// Patrón Facade: punto único para convocar, notificar cambios y publicar eventos académicos (RF-21, RF-27).
@Service
public class FachadaNotificaciones {

    private final NotificacionRepositorio repositorio;
    private final ResultadoRepositorio resultados;
    private final CatalogoAcademico catalogo;
    private final AccesoEvaluacion acceso;
    private final PublicadorEventos publicador;

    public FachadaNotificaciones(
            NotificacionRepositorio repositorio,
            ResultadoRepositorio resultados,
            CatalogoAcademico catalogo,
            AccesoEvaluacion acceso,
            PublicadorEventos publicador) {
        this.repositorio = repositorio;
        this.resultados = resultados;
        this.catalogo = catalogo;
        this.acceso = acceso;
        this.publicador = publicador;
    }

    @Transactional
    public ResumenEnvio convocar(Long practicaId) {
        ActorActual actor = acceso.exigirActor();
        acceso.exigirPermiso(actor, Permiso.PROGRAMAR_SESIONES);
        PracticaRef practica = acceso.exigirPracticaDocente(practicaId, actor);
        ResumenEnvio resumen = new ResumenEnvio(0, 0, 0);
        for (AutorizacionRef autorizacion : catalogo.autorizacionesDePractica(practica.id())) {
            resumen = resumen.acumular(publicador.publicar(eventoConvocatoria(practica, autorizacion.estudianteId())));
        }
        return resumen;
    }

    @Transactional
    public ResumenEnvio cambio(Long practicaId, CambioPracticaRequest request) {
        ActorActual actor = acceso.exigirActor();
        acceso.exigirPermiso(actor, Permiso.PROGRAMAR_SESIONES);
        PracticaRef practica = acceso.exigirPracticaDocente(practicaId, actor);
        String motivo = request.motivo().trim().toUpperCase(Locale.ROOT);
        if (motivo.isBlank()) {
            throw new ReglaNegocioException("Se requiere un motivo de cambio.");
        }
        return avisarCambio(practica, motivo, "");
    }

    /**
     * RF-21 sin actor: el módulo 3 cambió las condiciones de la sesión (evento SesionModificadaEvento).
     * La marca de tiempo hace que cada cambio genere su propio aviso (no se toma como duplicado).
     */
    @Transactional
    public ResumenEnvio notificarCambio(Long practicaId, String motivo, String marca) {
        return catalogo.buscarPractica(practicaId)
                .map(practica -> avisarCambio(practica, motivo, ":" + marca))
                .orElseGet(() -> new ResumenEnvio(0, 0, 0));
    }

    /**
     * RF-20 y RF-21 sin actor: el módulo 3 acaba de convocar a un estudiante. El correo lleva su código
     * de acceso; en la base solo queda el aviso SIN el código (el código solo existe como hash).
     */
    @Transactional
    public ResumenEnvio convocarEstudiante(Long practicaId, Long estudianteId, String codigoAcceso) {
        PracticaRef practica = catalogo.buscarPractica(practicaId).orElse(null);
        if (practica == null) {
            return new ResumenEnvio(0, 0, 0);
        }
        EventoAcademico base = eventoConvocatoria(practica, estudianteId);
        String instrucciones = " Ingresa con tu correo institucional y el código de acceso";
        EventoAcademico conCodigo = new EventoAcademico(
                base.tipo(),
                base.claveIdempotencia() + ":" + Integer.toHexString(codigoAcceso.hashCode()),
                base.practicaId(),
                base.estudianteId(),
                null,
                base.asunto(),
                base.cuerpo() + instrucciones + " que recibiste por correo.",
                base.cuerpo() + instrucciones + ": " + codigoAcceso);
        return publicador.publicar(conCodigo);
    }

    private ResumenEnvio avisarCambio(PracticaRef practica, String motivo, String sufijoClave) {
        ResumenEnvio resumen = new ResumenEnvio(0, 0, 0);
        String mensaje = practica.mensajePersonalizado() == null ? "" : practica.mensajePersonalizado();
        for (AutorizacionRef autorizacion : catalogo.autorizacionesDePractica(practica.id())) {
            String clave = "CAMBIO:" + practica.id() + ":" + motivo + sufijoClave + ":" + autorizacion.estudianteId();
            String cuerpo = "La practica '" + practica.nombre() + "' tuvo un cambio (" + motivo + "). "
                    + mensaje;
            resumen = resumen.acumular(publicador.publicar(new EventoAcademico(
                    TipoNotificacion.CAMBIO_PRACTICA,
                    clave,
                    practica.id(),
                    autorizacion.estudianteId(),
                    null,
                    "Cambio en practica " + practica.nombre(),
                    cuerpo)));
        }
        return resumen;
    }

    /**
     * RF-27: avisa calificaciones ya persistidas. No calcula ni reescribe notas.
     */
    @Transactional
    public ResumenEnvio publicarCalificaciones(Long practicaId) {
        ActorActual actor = acceso.exigirActor();
        acceso.exigirPermiso(actor, Permiso.CONSULTAR_PARTICIPACIONES);
        PracticaRef practica = acceso.exigirPracticaDocente(practicaId, actor);
        ResumenEnvio resumen = new ResumenEnvio(0, 0, 0);
        for (Resultado resultado : resultados.findAllByOrderByIdDesc()) {
            Participacion participacion = resultado.getParticipacion();
            if (!Objects.equals(participacion.getPracticaId(), practica.id())) {
                continue;
            }
            String cuerpo = "Se publico la calificacion del intento " + participacion.getNumeroIntento()
                    + " de '" + practica.nombre() + "'. Nota: " + resultado.getNotaFinal()
                    + " / " + resultado.getEscalaMaxima() + ".";
            resumen = resumen.acumular(publicador.publicar(new EventoAcademico(
                    TipoNotificacion.CALIFICACION,
                    "CALIFICACION:" + participacion.getId(),
                    practica.id(),
                    participacion.getEstudianteId(),
                    participacion.getId(),
                    "Calificacion publicada: " + practica.nombre(),
                    cuerpo)));
        }
        return resumen;
    }

    @Transactional(readOnly = true)
    public List<NotificacionRespuesta> listar(Long practicaId) {
        ActorActual actor = acceso.exigirActor();
        return repositorio.findAllByOrderByIdDesc().stream()
                .filter(n -> visible(actor, n))
                .filter(n -> practicaId == null || Objects.equals(n.getPracticaId(), practicaId))
                .map(this::toRespuesta)
                .toList();
    }

    private boolean visible(ActorActual actor, Notificacion n) {
        if (actor.esAdmin()) {
            return true;
        }
        if (actor.esEstudiante()) {
            return Objects.equals(n.getEstudianteId(), actor.estudianteId());
        }
        if (actor.esDocente()) {
            if (n.getPracticaId() == null) {
                return false;
            }
            return catalogo.buscarPractica(n.getPracticaId())
                    .map(p -> Objects.equals(p.docenteId(), actor.docenteId()))
                    .orElse(false);
        }
        return false;
    }

    private EventoAcademico eventoConvocatoria(PracticaRef practica, Long estudianteId) {
        EstudianteRef estudiante = catalogo.buscarEstudiante(estudianteId).orElse(null);
        String nombre = estudiante == null || estudiante.nombreCompleto() == null
                ? "estudiante"
                : estudiante.nombreCompleto();
        String extra = practica.mensajePersonalizado() == null ? "" : " " + practica.mensajePersonalizado();
        return new EventoAcademico(
                TipoNotificacion.CONVOCATORIA,
                "CONVOCATORIA:" + practica.id() + ":" + estudianteId,
                practica.id(),
                estudianteId,
                null,
                "Convocatoria: " + practica.nombre(),
                "Hola " + nombre + ", fuiste convocado a la practica '" + practica.nombre() + "'." + extra);
    }

    private NotificacionRespuesta toRespuesta(Notificacion n) {
        return new NotificacionRespuesta(
                n.getId(),
                n.getTipo(),
                n.getPracticaId(),
                n.getEstudianteId(),
                n.getParticipacionId(),
                n.getDestinatarioCorreo(),
                n.getAsunto(),
                n.getCuerpo(),
                n.getEstado(),
                n.getErrorMensaje(),
                n.getFechaCreacion(),
                n.getFechaEnvio());
    }
}
