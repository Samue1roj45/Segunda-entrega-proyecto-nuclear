package co.edu.humboldt.plataformacasos.sesiones.modulo3.negativas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.AutorizacionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.SesionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.GeneradorCodigoAcceso;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioGestionSesion;
import co.edu.humboldt.plataformacasos.sesiones.servicio.VerificadorCodigoAcceso;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// MÓDULO 3 · ESCENARIOS NEGATIVOS · RF-18 (consultar sesión) y RF-20 (validar código) 3
// Pareja exacta de ConsultaYAccesoPositivaTest.
class ConsultaYAccesoNegativaTest {

    @Test
    @DisplayName("CP-RF18-04 Rechazar la consulta de una sesión que no existe o no es del docente")
    void CP_RF18_04_rechazaSesionInexistente() {
        SesionRepositorio repo = mock(SesionRepositorio.class);
        when(repo.buscar(99L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ServicioGestionSesion(repo, mock(ContenidoCasoInterno.class),
                mock(ApplicationEventPublisher.class)).obtener(99L, 2L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("CP-RF20-02 Un código que no coincide, o de una sesión ya finalizada, no es válido")
    void CP_RF20_02_rechazaCodigoInvalido() {
        AutorizacionRepositorio repo = mock(AutorizacionRepositorio.class);
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        when(repo.hashesVigentes(3L)).thenReturn(List.of(bcrypt.encode("ABCD2345")));
        when(repo.hashesVigentes(4L)).thenReturn(List.of());   // su única sesión ya finalizó

        VerificadorCodigoAcceso verificador = new VerificadorCodigoAcceso(repo, bcrypt);

        assertThat(verificador.esValido(3L, "XXXXXXXX")).isFalse();
        assertThat(verificador.esValido(4L, "ABCD2345")).isFalse();
    }

    @Test
    @DisplayName("CP-RF20-04 No se aceptan códigos de acceso demasiado cortos")
    void CP_RF20_04_rechazaLongitudDeCodigoInsegura() {
        assertThatThrownBy(() -> new GeneradorCodigoAcceso(4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longitud-codigo");
    }
}
