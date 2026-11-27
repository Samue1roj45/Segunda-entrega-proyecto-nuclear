package co.edu.humboldt.plataformacasos.avisos;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Avisos propios del usuario que inició sesión y, solo para el administrador, el registro de auditoría. */
@RestController
@RequestMapping("/api")
public class AvisoControlador {

    public record AvisosRespuesta(List<AvisoDto> avisos, int noLeidos) {
    }

    /** {@code confirmada} debe ser true: la limpieza nunca se ejecuta sin confirmación explícita. */
    public record LimpiezaRequest(LocalDate antesDe, Boolean confirmada) {
    }

    private final ServicioAvisos avisos;
    private final ServicioAutorizacion autorizacion;
    private final LimpiezaAuditoria limpieza;

    public AvisoControlador(ServicioAvisos avisos, ServicioAutorizacion autorizacion, LimpiezaAuditoria limpieza) {
        this.avisos = avisos;
        this.autorizacion = autorizacion;
        this.limpieza = limpieza;
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
    public List<EntradaAuditoriaDto> auditoria(@RequestParam(defaultValue = "50") int limite,
                                               @RequestParam(defaultValue = "0") int desplazamiento,
                                               @RequestParam(required = false) String accion) {
        autorizacion.exigirPermiso(Permiso.ADMINISTRAR_DOCENTES);
        return avisos.auditoria(limite, desplazamiento, accion);
    }

    @GetMapping("/auditoria/acciones")
    public List<String> accionesAuditadas() {
        autorizacion.exigirPermiso(Permiso.ADMINISTRAR_DOCENTES);
        return avisos.accionesAuditadas();
    }

    @GetMapping("/auditoria/limpieza")
    public LimpiezaAuditoria.VistaPrevia vistaPreviaLimpieza(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate antesDe) {
        autorizacion.exigirPermiso(Permiso.ADMINISTRAR_DOCENTES);
        return limpieza.previsualizar(antesDe);
    }

    @PostMapping("/auditoria/limpieza")
    public LimpiezaAuditoria.Resultado limpiar(@RequestBody LimpiezaRequest solicitud) {
        var actor = autorizacion.exigirPermiso(Permiso.ADMINISTRAR_DOCENTES);
        if (solicitud == null || !Boolean.TRUE.equals(solicitud.confirmada())) {
            throw new SolicitudInvalidaException("Confirma la limpieza: la operación no se puede deshacer.");
        }
        return limpieza.ejecutar(solicitud.antesDe(), actor);
    }
}
