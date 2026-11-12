package co.edu.humboldt.plataformacasos.sesiones.proxy;

import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.sesiones.dto.ContextoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.EstadoIngresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ProgresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.TiempoRestanteDto;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioParticipacionEstudiante;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
/*
 #REQUISITO RF-10 || RF-11 || RF-22 || RF-23
 Patrón: PROXY (proxy de protección)
 Para qué sirve: antes de delegar al servicio real verifica que la participación (o la
 autorización) pertenezca al estudiante que hace la petición (el del token del módulo 1).
 Así un estudiante no puede leer ni cambiar datos de otro. Es @Primary: el controlador recibe
 este proxy sin enterarse.
 */
@Service
@Primary
public class ProxyServicioParticipacionEstudiante implements ServicioParticipacionEstudiante {

    private final ServicioParticipacionEstudiante real;
    private final ParticipacionEstudianteRepositorio repo;

    public ProxyServicioParticipacionEstudiante(@Qualifier("servicioParticipacionReal") ServicioParticipacionEstudiante real,
                                                ParticipacionEstudianteRepositorio repo) {
        this.real = real;
        this.repo = repo;
    }

    @Override
    public EstadoIngresoDto consultarIngreso(Long idEstudiante, Long idAutorizacion) {
        exigirAutorizacionPropia(idEstudiante, idAutorizacion);
        return real.consultarIngreso(idEstudiante, idAutorizacion);
    }

    @Override
    public ContextoDto consultarContexto(Long idEstudiante, Long idParticipacion) {
        exigirPropietario(idEstudiante, idParticipacion);
        return real.consultarContexto(idEstudiante, idParticipacion);
    }

    @Override
    public ProgresoDto responder(Long idEstudiante, Long idParticipacion, Long idPregunta, Long idRespuesta) {
        exigirPropietario(idEstudiante, idParticipacion);
        return real.responder(idEstudiante, idParticipacion, idPregunta, idRespuesta);
    }

    @Override
    public ProgresoDto consultarProgreso(Long idEstudiante, Long idParticipacion) {
        exigirPropietario(idEstudiante, idParticipacion);
        return real.consultarProgreso(idEstudiante, idParticipacion);
    }

    @Override
    public TiempoRestanteDto consultarTiempo(Long idEstudiante, Long idParticipacion) {
        exigirPropietario(idEstudiante, idParticipacion);
        return real.consultarTiempo(idEstudiante, idParticipacion);
    }

    private void exigirAutorizacionPropia(Long idEstudiante, Long idAutorizacion) {
        if (!repo.autorizacionDeEstudiante(idAutorizacion, idEstudiante)) {
            throw new AccesoDenegadoException("Esa autorización no pertenece al estudiante");
        }
    }

    private void exigirPropietario(Long idEstudiante, Long idParticipacion) {
        if (!repo.participacionDeEstudiante(idParticipacion, idEstudiante)) {
            throw new AccesoDenegadoException("La participación no existe o no pertenece al estudiante");
        }
    }
}
