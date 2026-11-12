package co.edu.humboldt.plataformacasos.sesiones.integracion;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Autorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.AutorizacionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.SesionRepositorio;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioConsultaSesiones implements ConsultaSesiones {

    private final SesionRepositorio sesiones;
    private final AutorizacionRepositorio autorizaciones;

    public ServicioConsultaSesiones(SesionRepositorio sesiones, AutorizacionRepositorio autorizaciones) {
        this.sesiones = sesiones;
        this.autorizaciones = autorizaciones;
    }

    @Override
    public Optional<Practica> sesion(Long idPractica) {
        return sesiones.buscar(idPractica);
    }

    @Override
    public Optional<Autorizacion> autorizacion(Long idAutorizacion) {
        return autorizaciones.buscar(idAutorizacion);
    }

    @Override
    public List<Autorizacion> autorizacionesActivas(Long idPractica) {
        return autorizaciones.activasDePractica(idPractica);
    }

    @Override
    public int intentosPermitidos(Long idAutorizacion) {
        return 1 + autorizaciones.contarRepeticiones(idAutorizacion);
    }
}
