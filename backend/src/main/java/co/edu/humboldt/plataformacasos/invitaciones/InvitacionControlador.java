package co.edu.humboldt.plataformacasos.invitaciones;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.invitaciones.ModeloInvitaciones.*;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Invitaciones de estudiantes. Las rutas /publica/** no llevan sesión: el token del enlace es la credencial. */
@RestController
@RequestMapping("/api/invitaciones")
public class InvitacionControlador {

    private final ServicioInvitaciones invitaciones;
    private final ServicioAutorizacion autorizacion;

    public InvitacionControlador(ServicioInvitaciones invitaciones, ServicioAutorizacion autorizacion) {
        this.invitaciones = invitaciones;
        this.autorizacion = autorizacion;
    }

    private long docente() {
        return autorizacion.exigirPermiso(Permiso.INVITAR_ESTUDIANTES).id();
    }

    @PostMapping
    public List<ResultadoInvitacion> invitar(@RequestBody SolicitudInvitar solicitud) {
        return invitaciones.invitar(docente(), solicitud.correos());
    }

    @GetMapping
    public List<InvitacionDocente> listar() {
        return invitaciones.listar(docente());
    }

    @PostMapping("/{id}/reenviar")
    public ResultadoInvitacion reenviar(@PathVariable long id) {
        return invitaciones.reenviar(docente(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable long id) {
        invitaciones.cancelar(docente(), id);
    }

    // ---- Público ----

    @GetMapping("/publica/{token}")
    public InvitacionPublica consultar(@PathVariable String token) {
        return invitaciones.consultar(token);
    }

    @PostMapping("/publica/{token}/aceptar")
    @ResponseStatus(HttpStatus.CREATED)
    public CuentaCreada aceptar(@PathVariable String token, @RequestBody DatosRegistro datos) {
        return invitaciones.aceptar(token, datos);
    }
}
