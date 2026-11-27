package co.edu.humboldt.plataformacasos.autenticacion.google;

import co.edu.humboldt.plataformacasos.autenticacion.servicio.CredencialesInvalidasException;
import co.edu.humboldt.plataformacasos.autenticacion.token.EmisorToken;
import co.edu.humboldt.plataformacasos.autenticacion.token.SesionUsuario;
import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioAutenticacionGoogleTest {

    private final VerificadorGoogle verificador = mock(VerificadorGoogle.class);
    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final EmisorToken emisor = mock(EmisorToken.class);
    private final ServicioAvisos avisos = mock(ServicioAvisos.class);
    private final SesionUsuario sesion = new SesionUsuario("t", "Bearer", null, "a@unihumboldt.edu.co", "Ana", "DOCENTE", java.util.Set.of());

    private ServicioAutenticacionGoogle servicio(String clientId) {
        return new ServicioAutenticacionGoogle(new PropiedadesGoogle(clientId, List.of("unihumboldt.edu.co", "cue.edu.co")),
                verificador, usuarios, emisor, avisos);
    }

    @Test @DisplayName("Sin ID de cliente configurado no se ofrece el ingreso")
    void sinConfigurar() {
        assertThatThrownBy(() -> servicio("").autenticar("x")).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test @DisplayName("Un token que Google no valida se rechaza")
    void tokenInvalido() {
        when(verificador.verificar("malo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio("id").autenticar("malo")).isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test @DisplayName("Correo no verificado por Google se rechaza")
    void noVerificado() {
        when(verificador.verificar("t")).thenReturn(Optional.of(new VerificadorGoogle.IdentidadGoogle("a@unihumboldt.edu.co", false)));
        assertThatThrownBy(() -> servicio("id").autenticar("t")).isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test @DisplayName("Un correo de otro dominio (gmail) no entra aunque Google lo valide")
    void otroDominio() {
        when(verificador.verificar("t")).thenReturn(Optional.of(new VerificadorGoogle.IdentidadGoogle("alguien@gmail.com", true)));
        assertThatThrownBy(() -> servicio("id").autenticar("t")).hasMessageContaining("unihumboldt.edu.co");
    }

    @Test @DisplayName("Dominio permitido pero sin cuenta en SOMA: se pide invitación")
    void sinCuenta() {
        when(verificador.verificar("t")).thenReturn(Optional.of(new VerificadorGoogle.IdentidadGoogle("nuevo@cue.edu.co", true)));
        when(usuarios.findByCorreoInstitucional("nuevo@cue.edu.co")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio("id").autenticar("t")).hasMessageContaining("invitación");
    }

    @Test @DisplayName("Con cuenta activa y dominio permitido entra y recibe el token de SOMA")
    void entra() {
        Usuario u = mock(Usuario.class);
        when(u.isActivo()).thenReturn(true);
        when(u.esPersonal()).thenReturn(true);
        when(verificador.verificar("t")).thenReturn(Optional.of(new VerificadorGoogle.IdentidadGoogle("A@UniHumboldt.edu.co", true)));
        when(usuarios.findByCorreoInstitucional("a@unihumboldt.edu.co")).thenReturn(Optional.of(u));
        when(emisor.emitir(any())).thenReturn(sesion);
        assertThat(servicio("id").autenticar("t")).isSameAs(sesion);
    }
}
