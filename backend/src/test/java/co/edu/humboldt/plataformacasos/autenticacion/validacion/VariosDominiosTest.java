package co.edu.humboldt.plataformacasos.autenticacion.validacion;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VariosDominiosTest {

    private final ValidadorDominioInstitucional validador = new ValidadorDominioInstitucional("unihumboldt.edu.co, cue.edu.co");

    @Test
    @DisplayName("Acepta cualquiera de los dominios configurados y rechaza el resto")
    void aceptaVariosDominios() {
        assertThat(validador.validar(new SolicitudAutenticacion("ana@unihumboldt.edu.co", "x")).aprobado()).isTrue();
        assertThat(validador.validar(new SolicitudAutenticacion("luis@CUE.edu.co", "x")).aprobado()).isTrue();
        var rechazo = validador.validar(new SolicitudAutenticacion("alguien@gmail.com", "x"));
        assertThat(rechazo.aprobado()).isFalse();
        assertThat(rechazo.mensaje()).contains("@unihumboldt.edu.co o @cue.edu.co");
    }
}
