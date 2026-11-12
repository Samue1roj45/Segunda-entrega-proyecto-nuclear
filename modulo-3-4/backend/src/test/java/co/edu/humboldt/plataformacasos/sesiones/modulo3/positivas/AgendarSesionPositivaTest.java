package co.edu.humboldt.plataformacasos.sesiones.modulo3.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.casos.integracion.ConsultaCasosProgramables;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RegistroParticipaciones;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import co.edu.humboldt.plataformacasos.sesiones.dto.AgendarSesionRequest;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionCreadaResponse;
import co.edu.humboldt.plataformacasos.sesiones.evento.EstudianteConvocadoEvento;
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
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

// MÓDULO 3 · ESCENARIOS POSITIVOS · RF-04 (agendar con estudiantes) 2
class AgendarSesionPositivaTest {

    @Test
    @DisplayName("CP-RF04-07 Agendar la sesión y asociar a cada estudiante con su propio código")
    void CP_RF04_07_agendaYAsociaCadaEstudiante() {
        SesionRepositorio sesiones = mock(SesionRepositorio.class);
        AutorizacionRepositorio autorizaciones = mock(AutorizacionRepositorio.class);
        ConsultaCasosProgramables casos = mock(ConsultaCasosProgramables.class);
        UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
        GeneradorCodigoAcceso generador = mock(GeneradorCodigoAcceso.class);
        PasswordEncoder codificador = mock(PasswordEncoder.class);
        ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
        ServicioSesiones service = new ServicioSesiones(sesiones, autorizaciones, mock(RepeticionRepositorio.class),
                casos, mock(ContenidoCasoInterno.class), usuarios, mock(RegistroParticipaciones.class), generador,
                codificador, eventos, Clock.systemDefaultZone());
        SesionProgramada s = Datos.sesionBase().build();
        when(casos.consultar(1L)).thenReturn(Datos.casoPublicado(2L));
        when(sesiones.agendar(s)).thenReturn(10L);
        when(sesiones.buscar(10L, 2L)).thenReturn(Optional.of(Datos.practica("SIN_INICIAR")));
        when(usuarios.findById(any())).thenReturn(Optional.of(Usuario.nuevoEstudiante("est@institucion.edu.co", "Est")));
        when(generador.generar()).thenReturn("ABCD2345", "WXYZ6789");
        when(codificador.encode(anyString())).thenReturn("hash");
        when(autorizaciones.insertar(eq(10L), any(), eq("hash"))).thenReturn(1L, 2L);
        when(autorizaciones.dePractica(10L)).thenReturn(List.of(Datos.autorizacion(1L, 3L, "ACTIVA"),
                Datos.autorizacion(2L, 4L, "ACTIVA")));

        SesionCreadaResponse r = service.agendarConEstudiantes(s);

        assertThat(r.idPractica()).isEqualTo(10L);
        assertThat(r.convocados()).hasSize(2);
        verify(autorizaciones).insertar(10L, 3L, "hash");
        verify(autorizaciones).insertar(10L, 4L, "hash");
        ArgumentCaptor<EstudianteConvocadoEvento> captor = ArgumentCaptor.forClass(EstudianteConvocadoEvento.class);
        verify(eventos, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).extracting(EstudianteConvocadoEvento::codigoAcceso)
                .containsExactly("ABCD2345", "WXYZ6789");
    }

    @Test
    @DisplayName("CP-RF04-09 La fachada valida, arma la sesión y la delega al servicio")
    void CP_RF04_09_fachadaDelegaUnaSesionValida() {
        ServicioSesiones sesionService = mock(ServicioSesiones.class);
        FachadaSesiones facade = new FachadaSesiones(new CadenaValidacionAgenda(), sesionService,
                mock(ServicioGestionSesion.class));
        AgendarSesionRequest req = new AgendarSesionRequest(1L, "Física Nuclear I", Datos.AHORA.plusHours(1),
                Datos.AHORA.plusHours(3), 60, "Laboratorio 3", null, null, List.of(3L, 4L));
        SesionCreadaResponse esperado = new SesionCreadaResponse(10L, List.of());
        when(sesionService.agendarConEstudiantes(any(SesionProgramada.class))).thenReturn(esperado);

        SesionCreadaResponse r = facade.agendar(2L, req);

        assertThat(r).isSameAs(esperado);
        ArgumentCaptor<SesionProgramada> captor = ArgumentCaptor.forClass(SesionProgramada.class);
        verify(sesionService).agendarConEstudiantes(captor.capture());
        assertThat(captor.getValue().idDocente()).isEqualTo(2L);
        assertThat(captor.getValue().idsEstudiantes()).containsExactly(3L, 4L);
    }
}
