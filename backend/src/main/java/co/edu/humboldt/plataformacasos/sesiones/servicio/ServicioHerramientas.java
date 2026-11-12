package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.evaluacion.participacion.EstadoParticipacion;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Herramienta;
import co.edu.humboldt.plataformacasos.sesiones.dto.HerramientaDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.RegistrarHerramientaRequest;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.HerramientasRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-28
 Para qué sirve: (1) el docente creador del caso define las herramientas profesionales del caso y
 (2) el estudiante las consulta y usa SOLO mientras su participación está en curso y solo si la
 participación es suya. Si el caso no definió herramientas, el estudiante recibe una lista vacía.
 */
@Service
public class ServicioHerramientas {

    private final HerramientasRepositorio repo;
    private final ParticipacionEstudianteRepositorio participaciones;
    private final ContenidoCasoInterno casos;

    public ServicioHerramientas(HerramientasRepositorio repo, ParticipacionEstudianteRepositorio participaciones,
                                ContenidoCasoInterno casos) {
        this.repo = repo;
        this.participaciones = participaciones;
        this.casos = casos;
    }

    @Transactional(readOnly = true)
    public List<HerramientaDto> consultar(Long idEstudiante, Long idParticipacion) {
        if (!participaciones.participacionDeEstudiante(idParticipacion, idEstudiante)) {
            throw new AccesoDenegadoException("La participación no existe o no pertenece al estudiante");
        }
        if (!EstadoParticipacion.EN_CURSO.name().equals(participaciones.estadoParticipacion(idParticipacion))) {
            throw new ReglaNegocioException("Las herramientas solo están disponibles mientras la participación está en curso");
        }
        return repo.delCaso(participaciones.idCasoDeParticipacion(idParticipacion)).stream()
                .map(HerramientaDto::de).toList();
    }

    @Transactional(readOnly = true)
    public List<HerramientaDto> listarDelCaso(Long idDocente, Long idCaso) {
        exigirCreador(idDocente, idCaso);
        return repo.delCaso(idCaso).stream().map(HerramientaDto::de).toList();
    }

    @Transactional
    public HerramientaDto registrar(Long idDocente, Long idCaso, RegistrarHerramientaRequest r) {
        exigirCreador(idDocente, idCaso);
        int orden = r.orden() != null ? r.orden() : repo.delCaso(idCaso).size() + 1;
        Herramienta nueva = new Herramienta(null, idCaso, r.nombre().trim(), r.tipo(), r.descripcion(),
                r.contenido(), orden);
        Long id = repo.registrar(nueva);
        return HerramientaDto.de(new Herramienta(id, idCaso, nueva.nombre(), nueva.tipo(), nueva.descripcion(),
                nueva.contenido(), orden));
    }

    @Transactional
    public void eliminar(Long idDocente, Long idCaso, Long idHerramienta) {
        exigirCreador(idDocente, idCaso);
        if (!repo.eliminar(idCaso, idHerramienta)) {
            throw new RecursoNoEncontradoException("La herramienta no existe en este caso");
        }
    }

    /** Solo el docente que creó el caso (módulo 2) define sus herramientas; el caso debe estar publicado. */
    private void exigirCreador(Long idDocente, Long idCaso) {
        if (!idDocente.equals(casos.resumen(idCaso).docenteId())) {
            throw new AccesoDenegadoException("Solo el docente creador del caso puede definir sus herramientas");
        }
    }
}
