package com.example.demo.modulo3.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.AgendarSesionRequest;
import com.example.demo.dto.ConvocadoDto;
import com.example.demo.dto.SesionCreadaResponse;
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
import org.mockito.ArgumentCaptor;

// MÓDULO 3 · ESCENARIOS POSITIVOS · RF-04 (asociar estudiantes y agendar) 2
// Patrón probado: Facade (ProgramacionFacade) sobre Builder + Chain + Service.
class AgendarSesionPositivaTest {

    @Test
    @DisplayName("CP-RF04-07 Agendar la sesión y asociar a cada estudiante")
    void CP_RF04_07_agendaYAsociaCadaEstudiante() {
        SesionRepository repo = mock(SesionRepository.class);
        SesionService service = new SesionService(repo);
        SesionProgramada s = Datos.sesionBase().build();
        when(repo.agendar(s)).thenReturn(10L);
        when(repo.listarConvocados(10L, 2L)).thenReturn(List.of(
                new ConvocadoDto(1L, 3L, "Estudiante Uno", "est1@test.edu", "ACTIVA"),
                new ConvocadoDto(2L, 4L, "Estudiante Dos", "est2@test.edu", "ACTIVA")));

        SesionCreadaResponse r = service.agendarConEstudiantes(s);

        assertThat(r.idPractica()).isEqualTo(10L);
        assertThat(r.convocados()).hasSize(2);
        verify(repo).agregarEstudiante(10L, 2L, 3L);
        verify(repo).agregarEstudiante(10L, 2L, 4L);
    }

    @Test
    @DisplayName("CP-RF04-09 La fachada valida, arma la sesión y la delega al servicio")
    void CP_RF04_09_fachadaDelegaUnaSesionValida() {
        SesionService sesionService = mock(SesionService.class);
        Facade facade = new Facade(new CadenaValidacionAgenda(), sesionService,
                mock(GestionSesionService.class));
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
