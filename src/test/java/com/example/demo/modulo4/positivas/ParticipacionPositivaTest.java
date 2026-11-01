package com.example.demo.modulo4.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.ContextoDto;
import com.example.demo.dto.ProgresoDto;
import com.example.demo.dto.ResponderRequest;
import com.example.demo.event.RespuestaRegistradaEvent;
import com.example.demo.proxy.ParticipacionServiceProxy;
import com.example.demo.test.Datos;
import com.example.demo.repository.ParticipacionRepository;
import com.example.demo.service.IngresoTardioService;
import com.example.demo.service.ParticipacionService;
import com.example.demo.service.ParticipacionServiceImpl;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

// MÓDULO 4 · ESCENARIOS POSITIVOS · RF-10 (responder y modificar) y RF-11 (respuestas y progreso) 6
// Patrones probados: Proxy (control de acceso) y Observer (evento al guardar una respuesta).
class ParticipacionPositivaTest {

    private final ParticipacionRepository repo = mock(ParticipacionRepository.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final IngresoTardioService ingreso = mock(IngresoTardioService.class);
    private final ParticipacionServiceImpl servicio = new ParticipacionServiceImpl(repo, eventos, ingreso);

    @Test
    @DisplayName("CP-RF10-01 Iniciar la participación validando antes el ingreso")
    void CP_RF10_01_iniciaLaParticipacionValidandoIngreso() {
        when(repo.iniciar(5L)).thenReturn(7L);

        Long id = servicio.iniciar(3L, 5L);

        assertThat(id).isEqualTo(7L);
        verify(ingreso).exigirPermitido(5L);
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
        ParticipacionService real = mock(ParticipacionService.class);
        ParticipacionRepository repoProxy = mock(ParticipacionRepository.class);
        ContextoDto contexto = new ContextoDto("Caso de prueba", "Contexto", "Desarrollo", List.of());
        when(repoProxy.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(real.consultarContexto(3L, 7L)).thenReturn(contexto);

        ContextoDto r = new ParticipacionServiceProxy(real, repoProxy).consultarContexto(3L, 7L);

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
        ArgumentCaptor<RespuestaRegistradaEvent> captor = ArgumentCaptor.forClass(RespuestaRegistradaEvent.class);
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
