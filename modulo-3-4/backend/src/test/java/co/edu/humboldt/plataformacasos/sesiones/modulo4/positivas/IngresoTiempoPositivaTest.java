package co.edu.humboldt.plataformacasos.sesiones.modulo4.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.sesiones.configuracion.ConfiguracionPoliticaIngreso;
import co.edu.humboldt.plataformacasos.sesiones.dto.EstadoIngresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.TiempoRestanteDto;
import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosTiempoSesion;
import co.edu.humboldt.plataformacasos.sesiones.politica.PoliticaMitadDelTiempo;
import co.edu.humboldt.plataformacasos.sesiones.politica.PoliticaPorcentajeConfigurable;
import co.edu.humboldt.plataformacasos.sesiones.soporte.Datos;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.tareas.TareaExpiracionParticipaciones;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioIngresoTardio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioParticipacionEstudianteImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
/*
 MÓDULO 4 · ESCENARIOS POSITIVOS · RF-22 (ingreso tardío) y RF-23 (tiempo máximo) 4
 Patrón probado: Strategy (políticas de ingreso).
 */
class IngresoTiempoPositivaTest {

    private static final LocalDateTime T0 = Datos.AHORA;

    @Test
    @DisplayName("CP-RF22-01 Permitir el ingreso dentro del plazo (mitad del tiempo máximo)")
    void CP_RF22_01_permiteIngresoDentroDelPlazo() {
        ParticipacionEstudianteRepositorio repo = mock(ParticipacionEstudianteRepositorio.class);
        when(repo.datosTiempo(5L)).thenReturn(Optional.of(
                new DatosTiempoSesion(T0, 60, null, "EN_CURSO", "ACTIVA", T0.plusMinutes(10))));

        EstadoIngresoDto e = new ServicioIngresoTardio(repo, new PoliticaMitadDelTiempo()).consultar(5L);

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
        assertThat(new ConfiguracionPoliticaIngreso().politicaIngresoTardio("mitad"))
                .isInstanceOf(PoliticaMitadDelTiempo.class);
    }

    @Test
    @DisplayName("CP-RF23-01 Consultar el tiempo restante de una participación en curso")
    void CP_RF23_01_consultaTiempoRestante() {
        ParticipacionEstudianteRepositorio repo = mock(ParticipacionEstudianteRepositorio.class);
        TiempoRestanteDto dto = new TiempoRestanteDto(7L, "EN_CURSO", T0, T0.plusMinutes(60), 3000L, false);
        when(repo.consultarTiempo(7L)).thenReturn(Optional.of(dto));
        ServicioParticipacionEstudianteImpl servicio = new ServicioParticipacionEstudianteImpl(repo,
                mock(ApplicationEventPublisher.class), mock(ServicioIngresoTardio.class));

        assertThat(servicio.consultarTiempo(3L, 7L)).isSameAs(dto);
    }

    @Test
    @DisplayName("CP-RF23-03 La expiración automática invoca el cierre de intentos vencidos")
    void CP_RF23_03_expiracionAutomaticaInvocaElCierre() {
        ParticipacionEstudianteRepositorio repo = mock(ParticipacionEstudianteRepositorio.class);

        new TareaExpiracionParticipaciones(repo).expirarVencidas();

        verify(repo).expirarVencidas();
    }
}
