package co.edu.humboldt.plataformacasos.autenticacion.validacion;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.IntentoAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;

/** Eslabón 3 (RF-01): el correo corresponde a un estudiante registrado y activo. */
public class ValidadorEstudianteRegistrado extends ValidadorBase<IntentoAutenticacion> {

    @Override
    protected ResultadoValidacion validarCondicion(IntentoAutenticacion intento) {
        boolean esEstudianteActivo = intento.usuario()
                .filter(usuario -> usuario.getRol() == Rol.ESTUDIANTE && usuario.isActivo())
                .isPresent();
        return esEstudianteActivo
                ? ResultadoValidacion.exito()
                : ResultadoValidacion.rechazo(MensajesAutenticacion.CREDENCIALES_INVALIDAS);
    }
}
