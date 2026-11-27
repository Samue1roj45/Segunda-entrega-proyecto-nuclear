package co.edu.humboldt.plataformacasos.invitaciones;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** app.invitaciones.*: adónde apunta el enlace del correo y cuánto vive la invitación. */
@ConfigurationProperties(prefix = "app.invitaciones")
public record PropiedadesInvitaciones(
        @DefaultValue("http://localhost:4200") String urlFrontend,
        @DefaultValue("7") int diasVigencia,
        @DefaultValue("14") int edadMinima) {
}
