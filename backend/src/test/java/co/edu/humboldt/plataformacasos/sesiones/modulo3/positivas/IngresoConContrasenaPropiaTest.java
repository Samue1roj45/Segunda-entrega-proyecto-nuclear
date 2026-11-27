package co.edu.humboldt.plataformacasos.sesiones.modulo3.positivas;

import co.edu.humboldt.plataformacasos.sesiones.repositorio.AutorizacionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.VerificadorCodigoAcceso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IngresoConContrasenaPropiaTest {

    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
    private final AutorizacionRepositorio repo = mock(AutorizacionRepositorio.class);
    private final VerificadorCodigoAcceso verificador = new VerificadorCodigoAcceso(repo, bcrypt);

    @Test
    @DisplayName("La contraseña propia distingue mayúsculas; el código de sesión no")
    void contrasenaYCodigo() {
        when(repo.claveHashDeEstudiante(7L)).thenReturn(Optional.of(bcrypt.encode("Clave-Propia-01")));
        when(repo.hashesDeEstudiante(7L)).thenReturn(List.of(bcrypt.encode("ABCD2345")));

        assertThat(verificador.esValido(7L, "Clave-Propia-01")).isTrue();
        assertThat(verificador.esValido(7L, "clave-propia-01")).isFalse();
        assertThat(verificador.esValido(7L, "abcd2345")).isTrue();      // código de sesión: sin importar mayúsculas
        assertThat(verificador.esValido(7L, "ABCD2345")).isTrue();
        assertThat(verificador.esValido(7L, "otra-cosa")).isFalse();
    }
}
