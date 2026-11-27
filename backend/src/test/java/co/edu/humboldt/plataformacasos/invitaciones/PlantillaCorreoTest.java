package co.edu.humboldt.plataformacasos.invitaciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaCorreoTest {

    @Test
    @DisplayName("El correo trae el botón con el enlace, el nombre del docente y escapa HTML")
    void contenido() throws Exception {
        String html = PlantillaCorreo.html("Te invitaron a SOMA", List.of("Samuel <b>Velásquez</b> te invitó."), "Crear mi cuenta",
                "http://localhost:4200/registro/abc_DEF-123", "Si no esperabas este correo, ignóralo.");
        assertThat(html).contains("href=\"http://localhost:4200/registro/abc_DEF-123\"").contains("Crear mi cuenta")
                .contains("Samuel &lt;b&gt;Vel&aacute;squez&lt;/b&gt; te invit&oacute;.").doesNotContain("<b>Velásquez</b>");
        if (System.getProperty("vistaCorreo") != null) {
            java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("vistaCorreo")), html);
        }
    }
}
