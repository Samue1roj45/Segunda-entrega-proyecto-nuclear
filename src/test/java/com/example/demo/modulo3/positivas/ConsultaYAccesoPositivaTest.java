package com.example.demo.modulo3.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.AccesoEstudianteRequest;
import com.example.demo.dto.AccesoEstudianteResponse;
import com.example.demo.dto.SesionDto;
import com.example.demo.test.Datos;
import com.example.demo.repository.AccesoRepository;
import com.example.demo.repository.SesionRepository;
import com.example.demo.service.AccesoEstudianteService;
import com.example.demo.service.GestionSesionService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MÓDULO 3 · ESCENARIOS POSITIVOS · RF-18 (consultar sesión) y RF-20 (validar código) 3
class ConsultaYAccesoPositivaTest {

    @Test
    @DisplayName("CP-RF18-03 Consultar una sesión propia y ver las acciones permitidas")
    void CP_RF18_03_consultaSesionConAccionesPermitidas() {
        SesionRepository repo = mock(SesionRepository.class);
        when(repo.buscar(1L, 2L)).thenReturn(Optional.of(Datos.sesionDto("SIN_INICIAR")));

        SesionDto s = new GestionSesionService(repo).obtener(1L, 2L);

        assertThat(s.estado()).isEqualTo("SIN_INICIAR");
        assertThat(s.accionesPermitidas()).containsExactly("INICIAR");
    }

    @Test
    @DisplayName("CP-RF20-01 Acceso con correo y código válidos (el código se normaliza)")
    void CP_RF20_01_accesoConCodigoValido() {
        AccesoRepository repo = mock(AccesoRepository.class);
        AccesoEstudianteResponse esperado = new AccesoEstudianteResponse(1L, 1L, 3L, "Estudiante Uno",
                "Caso de prueba", "EN_CURSO");
        when(repo.autenticarEstudiante("est1@test.edu", "ABC123")).thenReturn(esperado);

        AccesoEstudianteResponse r = new AccesoEstudianteService(repo).autenticar("  est1@test.edu ", " abc123 ");

        assertThat(r).isSameAs(esperado);
        verify(repo).autenticarEstudiante("est1@test.edu", "ABC123");
    }

    @Test
    @DisplayName("CP-RF20-03 Un request de acceso con correo y código completos es válido")
    void CP_RF20_03_requestDeAccesoValido() {
        var violaciones = Datos.validador().validate(new AccesoEstudianteRequest("est1@test.edu", "ABC123"));

        assertThat(violaciones).isEmpty();
    }
}
