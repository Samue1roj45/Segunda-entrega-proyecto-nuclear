package co.edu.humboldt.plataformacasos.autenticacion.validacion;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Eslabón 2 (RF-01): el correo pertenece al dominio institucional configurado. */
public class ValidadorDominioInstitucional extends ValidadorBase<SolicitudAutenticacion> {

    private final String dominio;
    private final Pattern correoDelDominio;

    public ValidadorDominioInstitucional(String dominioInstitucional) {
        Objects.requireNonNull(dominioInstitucional, "dominioInstitucional");
        this.dominio = dominioInstitucional.trim().toLowerCase(Locale.ROOT);
        if (dominio.isEmpty()) {
            throw new IllegalArgumentException("El dominio institucional no puede estar vacío");
        }
        this.correoDelDominio = Pattern.compile("^[^@\\s]+@" + Pattern.quote(dominio) + "$");
    }

    @Override
    protected ResultadoValidacion validarCondicion(SolicitudAutenticacion solicitud) {
        String correo = solicitud.correoInstitucional().trim().toLowerCase(Locale.ROOT);
        return correoDelDominio.matcher(correo).matches()
                ? ResultadoValidacion.exito()
                : ResultadoValidacion.rechazo(MensajesAutenticacion.correoNoInstitucional(dominio));
    }
}
