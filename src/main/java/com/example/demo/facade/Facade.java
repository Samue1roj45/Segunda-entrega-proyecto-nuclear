package com.example.demo.facade;

import com.example.demo.dto.AgendarSesionRequest;
import com.example.demo.dto.ConvocadoDto;
import com.example.demo.dto.SesionCreadaResponse;
import com.example.demo.dto.SesionDto;
import com.example.demo.model.SesionProgramada;
import com.example.demo.service.GestionSesionService;
import com.example.demo.service.SesionService;
import com.example.demo.service.validacion.CadenaValidacionAgenda;
import java.util.List;
import org.springframework.stereotype.Component;
/*
 #REQUISITO RF-04 || RF-18 || RF-19
 Patrón: FACADE
 Para qué sirve: punto de entrada único para programar y gestionar sesiones. Oculta al
 controlador los pasos internos: armar la sesión (Builder) -> validarla (Chain) ->
 guardarla (Service/BD) y el ciclo de vida (State). El controlador solo conoce esta clase.
 (Versión tanda 2: agrega consulta de sesiones e inicio/fin.)
 */
@Component
public class Facade {

    private final CadenaValidacionAgenda cadena;
    private final SesionService sesionService;
    private final GestionSesionService gestionService;

    public Facade(CadenaValidacionAgenda cadena, SesionService sesionService,
                              GestionSesionService gestionService) {
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
