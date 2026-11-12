package co.edu.humboldt.plataformacasos.sesiones.fachada;

import co.edu.humboldt.plataformacasos.sesiones.dto.AgendarSesionRequest;
import co.edu.humboldt.plataformacasos.sesiones.dto.ConvocadoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ModificarSesionRequest;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionCreadaResponse;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionDto;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioGestionSesion;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioSesiones;
import co.edu.humboldt.plataformacasos.sesiones.servicio.validacion.CadenaValidacionAgenda;
import java.util.List;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-04 || RF-18 || RF-19
 Patrón: FACADE
 Para qué sirve: punto de entrada único para programar y gestionar sesiones. Oculta al
 controlador los pasos internos: armar la sesión (Builder) -> validarla (Chain) ->
 guardarla (Service) y el ciclo de vida (State). El controlador solo conoce esta clase.
 */
@Component
public class FachadaSesiones {

    private final CadenaValidacionAgenda cadena;
    private final ServicioSesiones sesionService;
    private final ServicioGestionSesion gestionService;

    public FachadaSesiones(CadenaValidacionAgenda cadena, ServicioSesiones sesionService,
                              ServicioGestionSesion gestionService) {
        this.cadena = cadena;
        this.sesionService = sesionService;
        this.gestionService = gestionService;
    }

    // ----- RF-04 / RF-18: agendar -----
    public SesionCreadaResponse agendar(Long idDocente, AgendarSesionRequest r) {
        SesionProgramada sesion = SesionProgramada.builder()
                .idCaso(r.idCaso())
                .idDocente(idDocente)
                .espacioAcademico(r.espacioAcademico())
                .fechaInicio(r.fechaInicio())
                .fechaFin(r.fechaFin())
                .tiempoMaximoMinutos(r.tiempoMaximoMinutos())
                .lugar(r.lugar())
                .mensajePersonalizado(r.mensajePersonalizado())
                .urlAcceso(r.urlAcceso())
                .idsEstudiantes(r.idsEstudiantes())
                .build();
        cadena.validar(sesion);
        return sesionService.agendarConEstudiantes(sesion);
    }

    // ----- RF-18: cambiar condiciones (los convocados reciben el aviso, RF-21) -----
    public SesionDto modificarSesion(Long idPractica, Long idDocente, ModificarSesionRequest r) {
        SesionProgramada sesion = SesionProgramada.builder()
                .idCaso(gestionService.obtener(idPractica, idDocente).idCaso())
                .idDocente(idDocente)
                .espacioAcademico(r.espacioAcademico())
                .fechaInicio(r.fechaInicio())
                .fechaFin(r.fechaFin())
                .tiempoMaximoMinutos(r.tiempoMaximoMinutos())
                .lugar(r.lugar())
                .mensajePersonalizado(r.mensajePersonalizado())
                .urlAcceso(r.urlAcceso())
                .build();
        cadena.validarCondiciones(sesion);
        sesionService.modificarCondiciones(idPractica, sesion);
        return gestionService.obtener(idPractica, idDocente);
    }

    // ----- RF-04: estudiantes de la sesión -----
    public Long agregarEstudiante(Long idPractica, Long idDocente, Long idEstudiante) {
        return sesionService.agregarEstudiante(idPractica, idDocente, idEstudiante);
    }

    public void retirarEstudiante(Long idPractica, Long idDocente, Long idEstudiante) {
        sesionService.retirarEstudiante(idPractica, idDocente, idEstudiante);
    }

    public List<ConvocadoDto> listarConvocados(Long idPractica, Long idDocente) {
        return sesionService.listarConvocados(idPractica, idDocente);
    }

    // ----- RF-18: consultar sesiones -----
    public List<SesionDto> listarSesiones(Long idDocente, String estado) {
        return gestionService.listar(idDocente, estado);
    }

    public SesionDto obtenerSesion(Long idPractica, Long idDocente) {
        return gestionService.obtener(idPractica, idDocente);
    }

    // ----- RF-19: ciclo de vida -----
    public SesionDto iniciarSesion(Long idPractica, Long idDocente) {
        return gestionService.iniciar(idPractica, idDocente);
    }

    public SesionDto finalizarSesion(Long idPractica, Long idDocente) {
        return gestionService.finalizar(idPractica, idDocente);
    }
}
