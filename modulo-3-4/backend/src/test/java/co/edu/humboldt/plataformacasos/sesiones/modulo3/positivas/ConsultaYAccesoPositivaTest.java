package co.edu.humboldt.plataformacasos.sesiones.modulo3.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionDto;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.AutorizacionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.SesionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.GeneradorCodigoAcceso;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioGestionSesion;
import co.edu.humboldt.plataformacasos.sesiones.servicio.VerificadorCodigoAcceso;
import co.edu.humboldt.plataformacasos.sesiones.soporte.Datos;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// MÓDULO 3 · ESCENARIOS POSITIVOS · RF-18 (consultar sesión) y RF-20 (validar código) 3
class ConsultaYAccesoPositivaTest {

    @Test
    @DisplayName("CP-RF18-03 Consultar una sesión propia y ver las acciones permitidas")
    void CP_RF18_03_consultaSesionConAccionesPermitidas() {
        SesionRepositorio repo = mock(SesionRepositorio.class);
        ContenidoCasoInterno casos = mock(ContenidoCasoInterno.class);
        when(repo.buscar(1L, 2L)).thenReturn(Optional.of(Datos.practica("SIN_INICIAR")));
        when(casos.resumen(1L)).thenReturn(Datos.casoPublicado(2L));

        SesionDto s = new ServicioGestionSesion(repo, casos, mock(ApplicationEventPublisher.class)).obtener(1L, 2L);

        assertThat(s.estado()).isEqualTo("SIN_INICIAR");
        assertThat(s.nombreCaso()).isEqualTo("Caso de prueba");
        assertThat(s.accionesPermitidas()).containsExactly("INICIAR");
    }

    @Test
    @DisplayName("CP-RF20-01 El código de una autorización vigente es válido para el estudiante")
    void CP_RF20_01_accesoConCodigoValido() {
        AutorizacionRepositorio repo = mock(AutorizacionRepositorio.class);
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
        when(repo.hashesVigentes(3L)).thenReturn(List.of(bcrypt.encode("OTRO1234"), bcrypt.encode("ABCD2345")));

        assertThat(new VerificadorCodigoAcceso(repo, bcrypt).esValido(3L, "ABCD2345")).isTrue();
    }

    @Test
    @DisplayName("CP-RF20-03 El código generado es aleatorio, de 8 caracteres y sin caracteres ambiguos")
    void CP_RF20_03_codigoGeneradoValido() {
        GeneradorCodigoAcceso generador = new GeneradorCodigoAcceso(8);

        String codigo = generador.generar();

        assertThat(codigo).hasSize(8).matches("[A-HJ-NP-Z2-9]{8}");
        assertThat(generador.generar()).isNotEqualTo(codigo);
    }
}
