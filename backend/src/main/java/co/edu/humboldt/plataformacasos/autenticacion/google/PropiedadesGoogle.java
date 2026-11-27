package co.edu.humboldt.plataformacasos.autenticacion.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Ingreso con Google: ID de cliente OAuth (público) y dominios de correo que pueden entrar. */
@ConfigurationProperties(prefix = "app.google")
public record PropiedadesGoogle(String clientId, List<String> dominios) {

    public PropiedadesGoogle {
        clientId = clientId == null ? "" : clientId.trim();
        dominios = dominios == null ? List.of() : dominios.stream().map(d -> d.trim().toLowerCase()).filter(d -> !d.isEmpty()).toList();
    }

    public boolean configurado() { return !clientId.isEmpty(); }
}
