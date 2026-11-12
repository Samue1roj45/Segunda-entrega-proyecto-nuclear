package co.edu.humboldt.plataformacasos.sesiones.modulo3.negativas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.casos.integracion.ConsultaCasosProgramables;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RegistroParticipaciones;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import co.edu.humboldt.plataformacasos.sesiones.dto.AgendarSesionRequest;
import co.edu.humboldt.plataformacasos.sesiones.fachada.FachadaSesiones;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.AutorizacionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.RepeticionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.SesionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.GeneradorCodigoAcceso;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioGestionSesion;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioSesiones;
import co.edu.humboldt.plataformacasos.sesiones.servicio.validacion.CadenaValidacionAgenda;
import co.edu.humboldt.plataformacasos.sesiones.soporte.Datos;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

// MÓDULO 3 · ESCENARIOS NEGATIVOS · RF-04 (agendar con estudiantes) 2
// Pareja exacta de AgendarSesionPositivaTest.
class AgendarSesionNegativaTest {

    @Test
    @DisplayName("CP-RF04-08 Si un estudiante no puede asociarse, el error se propaga y no se continúa")
    void CP_RF04_08_propagaErrorAlAsociarEstudiante() {
        SesionRepositorio sesiones = mock(SesionRepositorio.class);
        AutorizacionRepositorio autorizaciones = mock(AutorizacionRepositorio.class);
        ConsultaCasosProgramables casos = mock(ConsultaCasosProgramables.class);
        UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
        GeneradorCodigoAcceso generador = mock(GeneradorCodigoAcceso.class);
        PasswordEncoder codificador = mock(PasswordEncoder.class);
        ServicioSesiones service = new ServicioSesiones(sesiones, autorizaciones, mock(RepeticionRepositorio.class),
                casos, mock(ContenidoCasoInterno.class), usuarios, mock(RegistroParticipaciones.class), generador,
                codificador, mock(ApplicationEventPublisher.class), Clock.systemDefaultZone());
        SesionProgramada s = Datos.sesionBase().build();
        when(casos.consultar(1L)).thenReturn(Datos.casoPublicado(2L));
        when(sesiones.agendar(s)).thenReturn(10L);
        when(usuarios.findById(3L)).thenReturn(Optional.of(Usuario.nuevoEstudiante("est@institucion.edu.co", "Est")));
        when(usuarios.findById(4L)).thenReturn(Optional.of(Usuario.nuevoDocente("doc@institucion.edu.co", "Doc")));
        when(generador.generar()).thenReturn("ABCD2345");
        when(codificador.encode(anyString())).thenReturn("hash");

        assertThatThrownBy(() -> service.agendarConEstudiantes(s))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no es un estudiante activo");
        verify(autorizaciones, never()).dePractica(anyLong());
    }

    @Test
    @DisplayName("CP-RF04-10 La fachada no llama al servicio si la solicitud es inválida")
    void CP_RF04_10_fachadaNoDelegaUnaSolicitudInvalida() {
        ServicioSesiones sesionService = mock(ServicioSesiones.class);
        FachadaSesiones facade = new FachadaSesiones(new CadenaValidacionAgenda(), sesionService,
                mock(ServicioGestionSesion.class));
        AgendarSesionRequest req = new AgendarSesionRequest(1L, "   ", Datos.AHORA.plusHours(1),
                Datos.AHORA.plusHours(3), 60, null, null, null, List.of(3L, 4L));

        assertThatThrownBy(() -> facade.agendar(2L, req))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("espacio académico");
        verify(sesionService, never()).agendarConEstudiantes(any(SesionProgramada.class));
    }

    @Test
    @DisplayName("CP-RF18-09 No se agenda si el tiempo máximo supera el tiempo estimado del caso")
    void CP_RF18_09_rechazaTiempoMayorAlEstimado() {
        ConsultaCasosProgramables casos = mock(ConsultaCasosProgramables.class);
        SesionRepositorio sesiones = mock(SesionRepositorio.class);
        ServicioSesiones service = new ServicioSesiones(sesiones, mock(AutorizacionRepositorio.class),
                mock(RepeticionRepositorio.class), casos, mock(ContenidoCasoInterno.class),
                mock(UsuarioRepositorio.class), mock(RegistroParticipaciones.class), mock(GeneradorCodigoAcceso.class),
                mock(PasswordEncoder.class), mock(ApplicationEventPublisher.class), Clock.systemDefaultZone());
        SesionProgramada s = Datos.sesionBase().fechaFin(Datos.AHORA.plusHours(5)).tiempoMaximoMinutos(120).build();
        when(casos.consultar(1L)).thenReturn(Datos.casoPublicado(2L));   // 90 minutos estimados

        assertThatThrownBy(() -> service.agendarConEstudiantes(s))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tiempo estimado del caso");
        verify(sesiones, never()).agendar(any());
    }
}
