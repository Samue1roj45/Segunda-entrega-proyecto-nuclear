package co.edu.humboldt.plataformacasos.sesiones.modulo4.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.sesiones.dto.ContextoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.EstadoIngresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ProgresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ResponderRequest;
import co.edu.humboldt.plataformacasos.sesiones.evento.RespuestaRegistradaEvento;
import co.edu.humboldt.plataformacasos.sesiones.proxy.ProxyServicioParticipacionEstudiante;
import co.edu.humboldt.plataformacasos.sesiones.soporte.Datos;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioIngresoTardio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioParticipacionEstudiante;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioParticipacionEstudianteImpl;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
/*
 MÓDULO 4 · ESCENARIOS POSITIVOS · RF-10 (responder y modificar) y RF-11 (respuestas y progreso) 6
 Patrones probados: Proxy (control de acceso) y Observer (evento al guardar una respuesta).
 */
class ParticipacionPositivaTest {

    private final ParticipacionEstudianteRepositorio repo = mock(ParticipacionEstudianteRepositorio.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final ServicioIngresoTardio ingreso = mock(ServicioIngresoTardio.class);
    private final ServicioParticipacionEstudianteImpl servicio = new ServicioParticipacionEstudianteImpl(repo, eventos, ingreso);

    @Test
    @DisplayName("CP-RF10-01 Antes de iniciar, el estudiante consulta si puede ingresar (regla de RF-22)")
    void CP_RF10_01_consultaElIngresoAntesDeIniciar() {
        EstadoIngresoDto permitido = new EstadoIngresoDto(true, null, Datos.AHORA.plusMinutes(30), Datos.AHORA,
                "EN_CURSO");
        when(ingreso.consultar(5L)).thenReturn(permitido);

        EstadoIngresoDto r = servicio.consultarIngreso(3L, 5L);

        assertThat(r.permitido()).isTrue();
        verify(ingreso).consultar(5L);
    }

    @Test
    @DisplayName("CP-RF10-03 Cambiar la respuesta de una pregunta ya respondida")
    void CP_RF10_03_cambiaLaRespuestaDeUnaPregunta() {
        ProgresoDto p = Datos.progreso(1, 2);
        when(repo.responder(7L, 1L, 1L)).thenReturn(p);
        when(repo.responder(7L, 1L, 2L)).thenReturn(p);

        servicio.responder(3L, 7L, 1L, 1L);
        servicio.responder(3L, 7L, 1L, 2L);

        verify(repo).responder(7L, 1L, 1L);
        verify(repo).responder(7L, 1L, 2L);
    }

    @Test
    @DisplayName("CP-RF10-05 El propietario de la participación consulta su contexto")
    void CP_RF10_05_propietarioConsultaSuContexto() {
        ServicioParticipacionEstudiante real = mock(ServicioParticipacionEstudiante.class);
        ParticipacionEstudianteRepositorio repoProxy = mock(ParticipacionEstudianteRepositorio.class);
        ContextoDto contexto = new ContextoDto("Caso de prueba", "Contexto", "Desarrollo", List.of());
        when(repoProxy.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(real.consultarContexto(3L, 7L)).thenReturn(contexto);

        ContextoDto r = new ProxyServicioParticipacionEstudiante(real, repoProxy).consultarContexto(3L, 7L);

        assertThat(r).isSameAs(contexto);
    }

    @Test
    @DisplayName("CP-RF10-07 Una respuesta con opción elegida es un request válido")
    void CP_RF10_07_requestDeRespuestaValido() {
        assertThat(Datos.validador().validate(new ResponderRequest(5L))).isEmpty();
    }

    @Test
    @DisplayName("CP-RF11-01 Guardar una respuesta devuelve el progreso y publica el evento")
    void CP_RF11_01_guardaRespuestaDevuelveProgresoYPublicaEvento() {
        when(repo.responder(7L, 1L, 1L)).thenReturn(Datos.progreso(1, 2));

        ProgresoDto p = servicio.responder(3L, 7L, 1L, 1L);

        assertThat(p.preguntasRespondidas()).isEqualTo(1);
        assertThat(p.porcentajeProgreso()).isEqualByComparingTo("50.00");
        ArgumentCaptor<RespuestaRegistradaEvento> captor = ArgumentCaptor.forClass(RespuestaRegistradaEvento.class);
        verify(eventos).publishEvent(captor.capture());
        assertThat(captor.getValue().idPregunta()).isEqualTo(1L);
        assertThat(captor.getValue().progreso().totalPreguntas()).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-RF11-03 Consultar el progreso de una participación existente")
    void CP_RF11_03_consultaElProgreso() {
        when(repo.consultarProgreso(7L)).thenReturn(Datos.progreso(2, 2));

        ProgresoDto p = servicio.consultarProgreso(3L, 7L);

        assertThat(p.porcentajeProgreso()).isEqualByComparingTo("100.00");
        verify(repo).consultarProgreso(7L);
    }
}
