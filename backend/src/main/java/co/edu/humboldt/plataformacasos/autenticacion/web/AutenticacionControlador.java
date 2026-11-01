package co.edu.humboldt.plataformacasos.autenticacion.web;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.servicio.ServicioAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.servicio.ServicioAutenticacionPersonal;
import co.edu.humboldt.plataformacasos.autenticacion.token.SesionUsuario;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos de inicio de sesión. */
@RestController
@RequestMapping("/api/auth")
public class AutenticacionControlador {

    private final ServicioAutenticacion servicioAutenticacion;
    private final ServicioAutenticacionPersonal servicioAutenticacionPersonal;

    public AutenticacionControlador(ServicioAutenticacion servicioAutenticacion,
                                    ServicioAutenticacionPersonal servicioAutenticacionPersonal) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.servicioAutenticacionPersonal = servicioAutenticacionPersonal;
    }

    /** RF-01: ingreso de estudiantes con correo institucional y código de autorización. */
    @PostMapping("/estudiantes/login")
    public SesionUsuario loginEstudiante(@RequestBody LoginEstudianteRequest solicitud) {
        return servicioAutenticacion.autenticarEstudiante(
                new SolicitudAutenticacion(solicitud.correoInstitucional(), solicitud.codigoAutorizacion()));
    }

    /** Ingreso de docentes y administrador con correo y contraseña (necesario para RF-02 y RF-03). */
    @PostMapping("/personal/login")
    public SesionUsuario loginPersonal(@RequestBody LoginPersonalRequest solicitud) {
        return servicioAutenticacionPersonal.autenticar(solicitud.correoInstitucional(), solicitud.clave());
    }
}
