package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;

import java.util.Objects;

import org.springframework.stereotype.Component;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActualResolver;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

@Component
public class AccesoEvaluacion {

    private final ActorActualResolver actores;
    private final CatalogoAcademico catalogo;
    private final ParticipacionRepositorio participaciones;

    public AccesoEvaluacion(
            ActorActualResolver actores,
            CatalogoAcademico catalogo,
            ParticipacionRepositorio participaciones) {
        this.actores = actores;
        this.catalogo = catalogo;
        this.participaciones = participaciones;
    }

    /**
     * RF-02: el actor debe tener el permiso del módulo 1 para la operación. El administrador conserva la
     * visibilidad total que este módulo le daba (puede operar como docente de cualquier práctica).
     */
    public void exigirPermiso(ActorActual actor, Permiso permiso) {
        if (!actor.esAdmin() && !actor.tienePermiso(permiso)) {
            throw new AccesoDenegadoException();
        }
    }

    /** Consultas: el estudiante necesita su permiso propio y el docente el permiso de consulta indicado. */
    public void exigirPermisoDeConsulta(ActorActual actor, Permiso permisoDocente) {
        exigirPermiso(actor, actor.esEstudiante() ? Permiso.CONSULTAR_RESULTADOS_PROPIOS : permisoDocente);
    }

    public ActorActual exigirActor() {
        return actores.actual().orElseThrow(() -> new AccesoDenegadoException("No se pudo resolver el actor autenticado."));
    }

    public boolean visible(ActorActual actor, Participacion participacion) {
        return visible(actor, participacion.getPracticaId(), participacion.getEstudianteId());
    }

    public boolean visible(ActorActual actor, Long practicaId, Long estudianteId) {
        if (actor.esAdmin()) {
            return true;
        }
        if (actor.esEstudiante()) {
            return Objects.equals(estudianteId, actor.estudianteId());
        }
        if (actor.esDocente()) {
            return catalogo.buscarPractica(practicaId)
                    .map(practica -> Objects.equals(practica.docenteId(), actor.docenteId()))
                    .orElse(false);
        }
        return false;
    }

    public void exigirVisible(Participacion participacion, ActorActual actor) {
        if (!visible(actor, participacion)) {
            throw new RecursoNoEncontradoException("Participación no encontrada.");
        }
    }

    public Participacion exigirParticipacion(Long id) {
        return participaciones.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Participación no encontrada."));
    }

    public Participacion exigirParticipacionVisible(Long id, ActorActual actor) {
        Participacion participacion = exigirParticipacion(id);
        if (!visible(actor, participacion)) {
            throw new RecursoNoEncontradoException("Participación no encontrada.");
        }
        return participacion;
    }

    /**
     * Mutaciones de intento (responder, finalizar): solo el estudiante dueño.
     */
    public Participacion exigirDueñoEstudiante(Long id, ActorActual actor) {
        Participacion participacion = exigirParticipacionVisible(id, actor);
        if (!actor.esEstudiante() || !Objects.equals(participacion.getEstudianteId(), actor.estudianteId())) {
            throw new AccesoDenegadoException("Solo el estudiante dueño puede responder o finalizar este intento.");
        }
        return participacion;
    }

    public PracticaRef exigirPracticaDocente(Long practicaId, ActorActual actor) {
        PracticaRef practica = catalogo.buscarPractica(practicaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica no encontrada."));
        if (actor.esAdmin()) {
            return practica;
        }
        if (actor.esDocente() && Objects.equals(practica.docenteId(), actor.docenteId())) {
            return practica;
        }
        throw new AccesoDenegadoException("No autorizado para esta práctica.");
    }
}
