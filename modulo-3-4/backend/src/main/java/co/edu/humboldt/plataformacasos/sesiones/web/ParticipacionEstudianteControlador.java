package co.edu.humboldt.plataformacasos.sesiones.web;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dto.AutorizacionEstudianteDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ContextoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.EstadoIngresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ProgresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ResponderRequest;
import co.edu.humboldt.plataformacasos.sesiones.dto.TiempoRestanteDto;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioParticipacionEstudiante;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioSesiones;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
/*
 #REQUISITO RF-10 || RF-11 || RF-20 || RF-22 || RF-23
 Para qué sirve: API REST del ESTUDIANTE: ver las sesiones a las que está convocado, saber si puede
 ingresar, ver contextos y preguntas, responder/cambiar respuestas y ver su progreso y tiempo restante.
 El estudiante es el usuario del token del módulo 1 (inició sesión con su correo y su código, RF-01/RF-20).
 Iniciar y finalizar el intento: POST /api/participaciones/iniciar y /{id}/finalizar (módulo 5, RF-12).
 Recibe la interfaz ServicioParticipacionEstudiante; Spring inyecta el Proxy (@Primary).
 */
@RestController
@RequestMapping("/api")
public class ParticipacionEstudianteControlador {

    private final ServicioParticipacionEstudiante service;
    private final ServicioSesiones sesiones;
    private final ServicioAutorizacion autorizacion;

    public ParticipacionEstudianteControlador(ServicioParticipacionEstudiante service, ServicioSesiones sesiones,
                                              ServicioAutorizacion autorizacion) {
        this.service = service;
        this.sesiones = sesiones;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/autorizaciones")                                  // RF-20: mis convocatorias
    public List<AutorizacionEstudianteDto> misAutorizaciones() {
        return sesiones.autorizacionesDeEstudiante(estudiante());
    }

    @GetMapping("/autorizaciones/{idAutorizacion}/ingreso")         // RF-22
    public EstadoIngresoDto ingreso(@PathVariable Long idAutorizacion) {
        return service.consultarIngreso(estudiante(), idAutorizacion);
    }

    @GetMapping("/participaciones/{id}/contexto")                   // RF-10
    public ContextoDto contexto(@PathVariable Long id) {
        return service.consultarContexto(estudiante(), id);
    }

    @PutMapping("/participaciones/{id}/preguntas/{idPregunta}/respuesta")   // RF-10 + RF-11
    public ProgresoDto responder(@PathVariable Long id, @PathVariable Long idPregunta,
                                 @Valid @RequestBody ResponderRequest req) {
        return service.responder(estudiante(), id, idPregunta, req.idRespuesta());
    }

    @GetMapping("/participaciones/{id}/progreso")                   // RF-11
    public ProgresoDto progreso(@PathVariable Long id) {
        return service.consultarProgreso(estudiante(), id);
    }

    @GetMapping("/participaciones/{id}/tiempo")                     // RF-23
    public TiempoRestanteDto tiempo(@PathVariable Long id) {
        return service.consultarTiempo(estudiante(), id);
    }

    private Long estudiante() {
        return autorizacion.exigirPermiso(Permiso.PARTICIPAR_EN_CASOS).id();
    }
}
