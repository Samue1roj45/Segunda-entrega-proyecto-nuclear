package co.edu.humboldt.plataformacasos.evaluacion.notificacion;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionControlador {

    private final FachadaNotificaciones facade;

    public NotificacionControlador(FachadaNotificaciones facade) {
        this.facade = facade;
    }

    @GetMapping
    public List<NotificacionRespuesta> listar(@RequestParam(required = false) Long practica) {
        return facade.listar(practica);
    }

    @PostMapping("/practica/{practicaId}/convocatoria")
    public ResumenEnvio convocar(@PathVariable Long practicaId) {
        return facade.convocar(practicaId);
    }

    @PostMapping("/practica/{practicaId}/cambio")
    public ResumenEnvio cambio(@PathVariable Long practicaId, @Valid @RequestBody CambioPracticaRequest request) {
        return facade.cambio(practicaId, request);
    }
}
