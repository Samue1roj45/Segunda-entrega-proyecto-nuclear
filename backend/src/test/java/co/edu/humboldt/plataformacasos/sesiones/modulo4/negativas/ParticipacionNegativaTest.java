package co.edu.humboldt.plataformacasos.sesiones.modulo4.negativas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.sesiones.dto.ResponderRequest;
import co.edu.humboldt.plataformacasos.sesiones.dto.TiempoRestanteDto;
import java.util.Optional;
import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.proxy.ProxyServicioParticipacionEstudiante;
import co.edu.humboldt.plataformacasos.sesiones.soporte.Datos;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioIngresoTardio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioParticipacionEstudiante;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioParticipacionEstudianteImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
/*
 MÓDULO 4 · ESCENARIOS NEGATIVOS · RF-10 (responder y modificar) y RF-11 (respuestas y progreso) 6
 Pareja exacta de ParticipacionPositivaTest
 */
class ParticipacionNegativaTest {

    private final ParticipacionEstudianteRepositorio repo = mock(ParticipacionEstudianteRepositorio.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final ServicioIngresoTardio ingreso = mock(ServicioIngresoTardio.class);
    private final ServicioParticipacionEstudianteImpl servicio = new ServicioParticipacionEstudianteImpl(repo, eventos, ingreso);

    @Test
    @DisplayName("CP-RF10-02 No se registra la respuesta si el tiempo máximo del intento ya se agotó")
    void CP_RF10_02_noRespondeConElTiempoAgotado() {
        when(repo.consultarTiempo(7L)).thenReturn(Optional.of(new TiempoRestanteDto(7L, "EN_CURSO",
                Datos.AHORA.minusMinutes(90), Datos.AHORA.minusMinutes(30), 0, true)));

        assertThatThrownBy(() -> servicio.responder(3L, 7L, 1L, 1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tiempo máximo");
        verify(repo, never()).responder(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("CP-RF10-04 Rechazar una opción que no pertenece a la pregunta")
    void CP_RF10_04_rechazaOpcionDeOtraPregunta() {
        when(repo.responder(7L, 1L, 99L)).thenThrow(new DataIntegrityViolationException("fk_registros_respuesta"));

        assertThatThrownBy(() -> servicio.responder(3L, 7L, 1L, 99L))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("CP-RF10-06 Un estudiante ajeno no puede consultar el contexto de otra participación")
    void CP_RF10_06_ajenoNoConsultaElContexto() {
        ServicioParticipacionEstudiante real = mock(ServicioParticipacionEstudiante.class);
        ParticipacionEstudianteRepositorio repoProxy = mock(ParticipacionEstudianteRepositorio.class);
        when(repoProxy.participacionDeEstudiante(7L, 4L)).thenReturn(false);

        assertThatThrownBy(() -> new ProxyServicioParticipacionEstudiante(real, repoProxy).consultarContexto(4L, 7L))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(real, never()).consultarContexto(anyLong(), anyLong());
    }

    @Test
    @DisplayName("CP-RF10-08 Una respuesta sin opción elegida no es un request válido")
    void CP_RF10_08_requestDeRespuestaSinOpcion() {
        var violaciones = Datos.validador().validate(new ResponderRequest(null));

        assertThat(violaciones).isNotEmpty();
        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("idRespuesta"));
    }

    @Test
    @DisplayName("CP-RF11-02 Si el tiempo se agotó, la respuesta se rechaza y no se publica el evento")
    void CP_RF11_02_rechazaRespuestaConElTiempoAgotado() {
        when(repo.responder(7L, 2L, 4L))
                .thenThrow(new ReglaNegocioException("Se agoto el tiempo maximo de participacion"));

        assertThatThrownBy(() -> servicio.responder(3L, 7L, 2L, 4L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tiempo maximo");
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("CP-RF11-04 Rechazar la consulta del progreso de una participación inexistente")
    void CP_RF11_04_rechazaProgresoDeParticipacionInexistente() {
        when(repo.consultarProgreso(99L)).thenThrow(new ReglaNegocioException("La participación no existe"));

        assertThatThrownBy(() -> servicio.consultarProgreso(3L, 99L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no existe");
    }
}
