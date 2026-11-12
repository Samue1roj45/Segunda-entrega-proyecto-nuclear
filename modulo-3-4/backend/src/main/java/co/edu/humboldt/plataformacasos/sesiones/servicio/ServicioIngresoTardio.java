package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.sesiones.dto.EstadoIngresoDto;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosTiempoSesion;
import co.edu.humboldt.plataformacasos.sesiones.politica.PoliticaIngresoTardio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-22
 Para qué sirve: decide si un estudiante puede iniciar su participación ahora, usando la
 estrategia de ingreso tardío. Lo usan el endpoint de consulta (para el frontend) y el módulo 5
 antes de registrar el inicio de un intento nuevo (RF-12), así la regla vive en un solo lugar.
 */
@Service
public class ServicioIngresoTardio {

    private final ParticipacionEstudianteRepositorio repo;
    private final PoliticaIngresoTardio politica;

    public ServicioIngresoTardio(ParticipacionEstudianteRepositorio repo, PoliticaIngresoTardio politica) {
        this.repo = repo;
        this.politica = politica;
    }

    @Transactional(readOnly = true)
    public EstadoIngresoDto consultar(Long idAutorizacion) {
        DatosTiempoSesion d = repo.datosTiempo(idAutorizacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La autorización no existe"));
        LocalDateTime limite = politica.limiteIngreso(d);

        String motivo = null;
        if (!"ACTIVA".equals(d.estadoAutorizacion())) {
            motivo = "La autorización del estudiante fue retirada";
        } else if ("FINALIZADO".equals(d.estadoSesion())) {
            motivo = "La sesión ya finalizó";
        } else if ("SIN_INICIAR".equals(d.estadoSesion())) {
            motivo = "La sesión aún no ha sido iniciada por el docente";
        } else if (d.ahora().isAfter(limite)) {
            motivo = "Ingreso tardío: ya pasó el límite para iniciar la participación";
        }
        return new EstadoIngresoDto(motivo == null, motivo, limite, d.ahora(), d.estadoSesion());
    }

    public void exigirPermitido(Long idAutorizacion) {
        EstadoIngresoDto estado = consultar(idAutorizacion);
        if (!estado.permitido()) {
            throw new ReglaNegocioException(estado.motivo());
        }
    }
}
