package com.example.demo.modulo4.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.configuracion.PoliticaIngresoConfig;
import com.example.demo.dto.EstadoIngresoDto;
import com.example.demo.dto.TiempoRestanteDto;
import com.example.demo.model.DatosTiempoSesion;
import com.example.demo.politica.PoliticaMitadDelTiempo;
import com.example.demo.politica.PoliticaPorcentajeConfigurable;
import com.example.demo.test.Datos;
import com.example.demo.repository.ParticipacionRepository;
import com.example.demo.scheduler.ExpiracionParticipacionesScheduler;
import com.example.demo.service.IngresoTardioService;
import com.example.demo.service.ParticipacionServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

// MÓDULO 4 · ESCENARIOS POSITIVOS · RF-22 (ingreso tardío) y RF-23 (tiempo máximo) 4
// Patrón probado: Strategy (políticas de ingreso).
class IngresoTiempoPositivaTest {

    private static final LocalDateTime T0 = Datos.AHORA;

    @Test
    @DisplayName("CP-RF22-01 Permitir el ingreso dentro del plazo (mitad del tiempo máximo)")
    void CP_RF22_01_permiteIngresoDentroDelPlazo() {
        ParticipacionRepository repo = mock(ParticipacionRepository.class);
        when(repo.datosTiempo(5L)).thenReturn(Optional.of(
                new DatosTiempoSesion(T0, 60, null, "EN_CURSO", "ACTIVA", T0.plusMinutes(10))));

        EstadoIngresoDto e = new IngresoTardioService(repo, new PoliticaMitadDelTiempo()).consultar(5L);

        assertThat(e.permitido()).isTrue();
        assertThat(e.motivo()).isNull();
        assertThat(e.limiteIngreso()).isEqualTo(T0.plusMinutes(30));
    }

    @Test
    @DisplayName("CP-RF22-03 Calcular el límite de ingreso con cada política")
    void CP_RF22_03_calculaElLimiteConCadaPolitica() {
        DatosTiempoSesion d = new DatosTiempoSesion(T0, 60, new BigDecimal("80"), "EN_CURSO", "ACTIVA", T0);

        assertThat(new PoliticaMitadDelTiempo().limiteIngreso(d)).isEqualTo(T0.plusMinutes(30));
        assertThat(new PoliticaPorcentajeConfigurable().limiteIngreso(d)).isEqualTo(T0.plusMinutes(48));
        assertThat(new PoliticaIngresoConfig().politicaIngresoTardio("mitad"))
                .isInstanceOf(PoliticaMitadDelTiempo.class);
    }

    @Test
    @DisplayName("CP-RF23-01 Consultar el tiempo restante de una participación en curso")
    void CP_RF23_01_consultaTiempoRestante() {
        ParticipacionRepository repo = mock(ParticipacionRepository.class);
        TiempoRestanteDto dto = new TiempoRestanteDto(7L, "EN_CURSO", T0, T0.plusMinutes(60), 3000L, false);
        when(repo.consultarTiempo(7L)).thenReturn(Optional.of(dto));
        ParticipacionServiceImpl servicio = new ParticipacionServiceImpl(repo,
                mock(ApplicationEventPublisher.class), mock(IngresoTardioService.class));

        assertThat(servicio.consultarTiempo(3L, 7L)).isSameAs(dto);
    }

    @Test
    @DisplayName("CP-RF23-03 La expiración automática invoca el cierre de intentos vencidos")
    void CP_RF23_03_expiracionAutomaticaInvocaElCierre() {
        ParticipacionRepository repo = mock(ParticipacionRepository.class);

        new ExpiracionParticipacionesScheduler(repo).expirarVencidas();

        verify(repo).expirarVencidas();
    }
}
