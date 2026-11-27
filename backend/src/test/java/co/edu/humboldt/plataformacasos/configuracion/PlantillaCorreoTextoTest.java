package co.edu.humboldt.plataformacasos.configuracion;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PlantillaCorreoTextoTest {

    @Test
    void destacaElCodigoDeAccesoDeLaConvocatoria() throws Exception {
        String html = PlantillaCorreoTexto.html("SOMA · Convocatoria a una sesión",
                "Fuiste convocado a la práctica 'Ecos de una decisión' el 28/11/2026 a las 10:00 en Laboratorio 3."
                        + " Ingresa con tu correo institucional y el código de acceso: K7QX2MPA",
                "https://front-lbjo.onrender.com");

        assertThat(html).contains(">K7QX2MPA<");
        assertThat(html).contains("Tu código de acceso");
        assertThat(html).contains("el código de acceso que aparece abajo.");
        assertThat(html).doesNotContain("acceso: K7QX2MPA");
        assertThat(html).contains("<h1").contains("Convocatoria a una sesión");
        assertThat(html).contains("href=\"https://front-lbjo.onrender.com\"");
        guardarVistaPrevia("correo-codigo.html", html);
    }

    @Test
    void unAvisoSimpleSeEscapaYSinBotonSiNoHayUrl() throws Exception {
        String html = PlantillaCorreoTexto.html("SOMA · Un estudiante ingresó",
                "Ana <Pérez> ingresó a la plataforma.\n\n— Plataforma SOMA", "");

        assertThat(html).contains("Ana &lt;Pérez&gt; ingresó a la plataforma.");
        assertThat(html).doesNotContain("— Plataforma SOMA");
        assertThat(html).doesNotContain("Ir a SOMA");
        assertThat(html).doesNotContain("Tu código de acceso");
        guardarVistaPrevia("correo-aviso.html", html);
    }

    /** Deja el HTML en target/ para revisarlo en un navegador (no afecta la prueba). */
    private static void guardarVistaPrevia(String nombre, String html) throws Exception {
        Path carpeta = Path.of("target", "vista-previa-correos");
        Files.createDirectories(carpeta);
        Files.writeString(carpeta.resolve(nombre), html);
    }
}
