package co.edu.humboldt.plataformacasos.autenticacion.validacion;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

// Patrón Chain of Responsibility: eslabón que exige el dominio institucional del correo.
/** Eslabón 2 (RF-01): el correo pertenece al dominio institucional configurado. */
public class ValidadorDominioInstitucional extends ValidadorBase<SolicitudAutenticacion> {

    private final String dominio;
    private final Pattern correoDelDominio;

    /** Acepta uno o varios dominios separados por coma (p. ej. «unihumboldt.edu.co,cue.edu.co»). */

    public ValidadorDominioInstitucional(String dominiosInstitucionales) {
        Objects.requireNonNull(dominiosInstitucionales, "dominioInstitucional");
        java.util.List<String> lista = java.util.Arrays.stream(dominiosInstitucionales.split(","))
                .map(d -> d.trim().toLowerCase(Locale.ROOT)).filter(d -> !d.isEmpty()).toList();
        if (lista.isEmpty()) {
            throw new IllegalArgumentException("El dominio institucional no puede estar vacío");
        }
        this.dominio = String.join(" o @", lista);
        this.correoDelDominio = Pattern.compile("^[^@\\s]+@(?:" + lista.stream().map(Pattern::quote).collect(java.util.stream.Collectors.joining("|")) + ")$");
    }

    @Override
    protected ResultadoValidacion validarCondicion(SolicitudAutenticacion solicitud) {
        String correo = solicitud.correoInstitucional().trim().toLowerCase(Locale.ROOT);
        return correoDelDominio.matcher(correo).matches()
                ? ResultadoValidacion.exito()
                : ResultadoValidacion.rechazo(MensajesAutenticacion.correoNoInstitucional(dominio));
    }
}
