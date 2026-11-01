package com.example.demo.modulo4.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.SesionDto;
import com.example.demo.test.Datos;
import com.example.demo.repository.SesionRepository;
import com.example.demo.scheduler.CierreAutomaticoScheduler;
import com.example.demo.service.GestionSesionService;
import com.example.demo.state.AccionesSesion;
import com.example.demo.state.SesionEnCurso;
import com.example.demo.state.SesionSinIniciar;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
/*
 MÓDULO 4 · ESCENARIOS POSITIVOS · RF-19 (estados de la sesión) 4
 Patrón probado: State (SesionSinIniciar, SesionEnCurso) y su contexto GestionSesionService.
 */
class EstadosSesionPositivaTest {

    @Test
    @DisplayName("CP-RF19-01 Una sesión SIN_INICIAR se puede iniciar")
    void CP_RF19_01_sesionSinIniciarSePuedeIniciar() {
        AccionesSesion acciones = mock(AccionesSesion.class);

        new SesionSinIniciar().iniciar(acciones, 1L, 2L);

        verify(acciones).iniciarEnBd(1L, 2L);
    }

    @Test
    @DisplayName("CP-RF19-03 Una sesión EN_CURSO se puede finalizar")
    void CP_RF19_03_sesionEnCursoSePuedeFinalizar() {
        AccionesSesion acciones = mock(AccionesSesion.class);

        new SesionEnCurso().finalizar(acciones, 1L, 2L);

        verify(acciones).finalizarEnBd(1L, 2L);
    }

    @Test
    @DisplayName("CP-RF19-05 El docente inicia su sesión y queda EN_CURSO")
    void CP_RF19_05_docenteIniciaSuSesion() {
        SesionRepository repo = mock(SesionRepository.class);
        when(repo.buscar(1L, 2L)).thenReturn(Optional.of(Datos.sesionDto("SIN_INICIAR")),
                Optional.of(Datos.sesionDto("EN_CURSO")));

        SesionDto r = new GestionSesionService(repo).iniciar(1L, 2L);

        verify(repo).iniciarEnBd(1L, 2L);
        assertThat(r.estado()).isEqualTo("EN_CURSO");
        assertThat(r.accionesPermitidas()).containsExactly("FINALIZAR");
    }

    @Test
    @DisplayName("CP-RF19-07 El cierre automático invoca el cierre de sesiones vencidas")
    void CP_RF19_07_cierreAutomaticoInvocaElCierre() {
        SesionRepository repo = mock(SesionRepository.class);

        new CierreAutomaticoScheduler(repo).cerrarVencidas();

        verify(repo).cerrarVencidas();
    }
}
