package com.example.demo.modulo3.negativas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.AgendarSesionRequest;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.facade.Facade;
import com.example.demo.model.SesionProgramada;
import com.example.demo.test.Datos;
import com.example.demo.repository.SesionRepository;
import com.example.demo.service.GestionSesionService;
import com.example.demo.service.SesionService;
import com.example.demo.service.validacion.CadenaValidacionAgenda;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MÓDULO 3 · ESCENARIOS NEGATIVOS · RF-04 (agendar con estudiantes) 2
// Pareja exacta de AgendarSesionPositivaTest.
class AgendarSesionNegativaTest {

    @Test
    @DisplayName("CP-RF04-08 Si un estudiante no puede asociarse, el error se propaga y no se continúa")
    void CP_RF04_08_propagaErrorAlAsociarEstudiante() {
        SesionRepository repo = mock(SesionRepository.class);
        SesionProgramada s = Datos.sesionBase().build();
        when(repo.agendar(s)).thenReturn(10L);
        when(repo.agregarEstudiante(10L, 2L, 4L)).thenThrow(
                new ReglaNegocioException("El usuario autorizado debe estar activo y tener rol ESTUDIANTE"));

        assertThatThrownBy(() -> new SesionService(repo).agendarConEstudiantes(s))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("rol ESTUDIANTE");
        verify(repo, never()).listarConvocados(anyLong(), anyLong());
    }

    @Test
    @DisplayName("CP-RF04-10 La fachada no llama al servicio si la solicitud es inválida")
    void CP_RF04_10_fachadaNoDelegaUnaSolicitudInvalida() {
        SesionService sesionService = mock(SesionService.class);
        Facade facade = new Facade(new CadenaValidacionAgenda(), sesionService,
                mock(GestionSesionService.class));
        AgendarSesionRequest req = new AgendarSesionRequest(1L, "   ", Datos.AHORA.plusHours(1),
                Datos.AHORA.plusHours(3), 60, null, null, null, List.of(3L, 4L));

        assertThatThrownBy(() -> facade.agendar(2L, req))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("espacio académico");
        verify(sesionService, never()).agendarConEstudiantes(any(SesionProgramada.class));
    }
}
