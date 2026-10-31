package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.AutorizacionRef;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.EstudianteRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.ReglaNegocioException;
import com.unihumboldt.nuclear.evaluacion.resultado.Resultado;
import com.unihumboldt.nuclear.evaluacion.resultado.ResultadoRepository;

@Service
public class NotificacionFacade {

    private final NotificacionRepository repositorio;
    private final ResultadoRepository resultados;
    private final CatalogoAcademico catalogo;
    private final AccesoEvaluacion acceso;
    private final PublicadorEventos publicador;

    public NotificacionFacade(
            NotificacionRepository repositorio,
            ResultadoRepository resultados,
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
        PracticaRef practica = acceso.exigirPracticaDocente(practicaId, actor);
        String motivo = request.motivo().trim().toUpperCase(Locale.ROOT);
        if (motivo.isBlank()) {
            throw new ReglaNegocioException("Se requiere un motivo de cambio.");
        }
        ResumenEnvio resumen = new ResumenEnvio(0, 0, 0);
        String mensaje = practica.mensajePersonalizado() == null ? "" : practica.mensajePersonalizado();
        for (AutorizacionRef autorizacion : catalogo.autorizacionesDePractica(practica.id())) {
            String clave = "CAMBIO:" + practica.id() + ":" + motivo + ":" + autorizacion.estudianteId();
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
