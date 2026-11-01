package com.example.demo.modulo4.negativas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.configuracion.PoliticaIngresoConfig;
import com.example.demo.dto.EstadoIngresoDto;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.DatosTiempoSesion;
import com.example.demo.politica.PoliticaMitadDelTiempo;
import com.example.demo.test.Datos;
import com.example.demo.repository.ParticipacionRepository;
import com.example.demo.scheduler.ExpiracionParticipacionesScheduler;
import com.example.demo.service.IngresoTardioService;
import com.example.demo.service.ParticipacionServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
/*
 MÓDULO 4 · ESCENARIOS NEGATIVOS · RF-22 (ingreso tardío) y RF-23 (tiempo máximo) 4
 Pareja exacta de IngresoTiempoPositivaTest.
 */
class IngresoTiempoNegativaTest {

    private static final LocalDateTime T0 = Datos.AHORA;

    @Test
    @DisplayName("CP-RF22-02 Bloquear el ingreso pasada la mitad del tiempo máximo")
    void CP_RF22_02_bloqueaIngresoTardio() {
        ParticipacionRepository repo = mock(ParticipacionRepository.class);
        when(repo.datosTiempo(5L)).thenReturn(Optional.of(
                new DatosTiempoSesion(T0, 60, null, "EN_CURSO", "ACTIVA", T0.plusMinutes(45))));

        EstadoIngresoDto e = new IngresoTardioService(repo, new PoliticaMitadDelTiempo()).consultar(5L);

        assertThat(e.permitido()).isFalse();
        assertThat(e.motivo()).contains("Ingreso tardío");
    }

    @Test
    @DisplayName("CP-RF22-04 Rechazar una política de ingreso desconocida en la configuración")
    void CP_RF22_04_rechazaPoliticaDesconocida() {
        assertThatThrownBy(() -> new PoliticaIngresoConfig().politicaIngresoTardio("otra"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mitad");
    }

    @Test
    @DisplayName("CP-RF23-02 Rechazar la consulta del tiempo de una participación inexistente")
    void CP_RF23_02_rechazaTiempoDeParticipacionInexistente() {
        ParticipacionRepository repo = mock(ParticipacionRepository.class);
        when(repo.consultarTiempo(99L)).thenReturn(Optional.empty());
        ParticipacionServiceImpl servicio = new ParticipacionServiceImpl(repo,
                mock(ApplicationEventPublisher.class), mock(IngresoTardioService.class));

        assertThatThrownBy(() -> servicio.consultarTiempo(3L, 99L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("CP-RF23-04 Si la expiración automática falla, el error no detiene la aplicación")
    void CP_RF23_04_falloDeLaExpiracionNoSePropaga() {
        ParticipacionRepository repo = mock(ParticipacionRepository.class);
        doThrow(new RuntimeException("BD no disponible")).when(repo).expirarVencidas();

        assertThatCode(() -> new ExpiracionParticipacionesScheduler(repo).expirarVencidas())
                .doesNotThrowAnyException();
    }
}
