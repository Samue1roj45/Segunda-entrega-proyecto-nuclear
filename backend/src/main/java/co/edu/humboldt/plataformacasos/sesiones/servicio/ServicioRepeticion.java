package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.sesiones.dto.IntentosDto;
import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SolicitudRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.RepeticionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.repeticion.CadenaValidacionRepeticion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-25
 Para qué sirve: el docente creador del caso concede un intento adicional a un estudiante
 y consulta cuántos intentos lleva y cuántos tiene permitidos.
 */
@Service
public class ServicioRepeticion {

    private final RepeticionRepositorio repo;
    private final CadenaValidacionRepeticion cadena;

    public ServicioRepeticion(RepeticionRepositorio repo, CadenaValidacionRepeticion cadena) {
        this.repo = repo;
        this.cadena = cadena;
    }

    @Transactional
    public IntentosDto autorizar(Long idAutorizacion, Long idDocente, String motivo) {
        DatosRepeticion datos = cargar(idAutorizacion);
        cadena.validar(new SolicitudRepeticion(idAutorizacion, idDocente, motivo, datos));
        repo.autorizar(idAutorizacion, idDocente, motivo);
        DatosRepeticion actualizados = cargar(idAutorizacion);
        return new IntentosDto(idAutorizacion, actualizados.intentosUsados(), actualizados.intentosPermitidos());
    }

    @Transactional(readOnly = true)
    public IntentosDto consultar(Long idAutorizacion, Long idDocente) {
        DatosRepeticion d = cargar(idAutorizacion);
        if (!idDocente.equals(d.idDocenteCreador()) && !idDocente.equals(d.idDocenteSesion())) {
            throw new AccesoDenegadoException("Solo el docente de la sesión o creador del caso puede consultar esto");
        }
        return new IntentosDto(idAutorizacion, d.intentosUsados(), d.intentosPermitidos());
    }

    private DatosRepeticion cargar(Long idAutorizacion) {
        return repo.cargar(idAutorizacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La autorización no existe"));
    }
}
