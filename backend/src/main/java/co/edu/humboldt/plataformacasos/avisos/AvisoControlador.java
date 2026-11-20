package co.edu.humboldt.plataformacasos.avisos;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Avisos propios del usuario que inició sesión y, solo para el administrador, el registro de auditoría. */
@RestController
@RequestMapping("/api")
public class AvisoControlador {

    public record AvisosRespuesta(List<AvisoDto> avisos, int noLeidos) {
    }

    private final ServicioAvisos avisos;
    private final ServicioAutorizacion autorizacion;

    public AvisoControlador(ServicioAvisos avisos, ServicioAutorizacion autorizacion) {
        this.avisos = avisos;
        this.autorizacion = autorizacion;
    }

    @GetMapping("/avisos")
    public AvisosRespuesta mios(@RequestParam(defaultValue = "30") int limite) {
        Long yo = autorizacion.verificarIdentidad().id();
        return new AvisosRespuesta(avisos.listar(yo, limite), avisos.noLeidos(yo));
    }

    @PostMapping("/avisos/{id}/leido")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leido(@PathVariable Long id) {
        avisos.marcarLeido(autorizacion.verificarIdentidad().id(), id);
    }

    @PostMapping("/avisos/leidos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void todosLeidos() {
        avisos.marcarTodosLeidos(autorizacion.verificarIdentidad().id());
    }

    @GetMapping("/auditoria")
    public List<EntradaAuditoriaDto> auditoria(@RequestParam(defaultValue = "100") int limite) {
        autorizacion.exigirPermiso(Permiso.ADMINISTRAR_DOCENTES);
        return avisos.auditoria(limite);
    }
}
