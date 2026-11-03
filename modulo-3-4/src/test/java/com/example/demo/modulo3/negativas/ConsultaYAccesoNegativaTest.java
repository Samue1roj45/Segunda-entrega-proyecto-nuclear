package com.example.demo.modulo3.negativas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.dto.AccesoEstudianteRequest;
import com.example.demo.exception.NoAutenticadoException;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.test.Datos;
import com.example.demo.repository.AccesoRepository;
import com.example.demo.repository.SesionRepository;
import com.example.demo.service.AccesoEstudianteService;
import com.example.demo.service.GestionSesionService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MÓDULO 3 · ESCENARIOS NEGATIVOS · RF-18 (consultar sesión) y RF-20 (validar código) 3
// Pareja exacta de ConsultaYAccesoPositivaTest.
class ConsultaYAccesoNegativaTest {

    @Test
    @DisplayName("CP-RF18-04 Rechazar la consulta de una sesión que no existe o no es del docente")
    void CP_RF18_04_rechazaSesionInexistente() {
        SesionRepository repo = mock(SesionRepository.class);
        when(repo.buscar(99L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GestionSesionService(repo).obtener(99L, 2L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("CP-RF20-02 Rechazar el acceso cuando la BD invalida el código (401)")
    void CP_RF20_02_rechazaCodigoInvalido() {
        AccesoRepository repo = mock(AccesoRepository.class);
        when(repo.autenticarEstudiante(anyString(), anyString())).thenThrow(
                Datos.errorBd45000("Correo o codigo de autorizacion invalidos, vencidos o sesion finalizada"));

        assertThatThrownBy(() -> new AccesoEstudianteService(repo).autenticar("est1@test.edu", "XXXXXXXXXX"))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessageContaining("invalidos");
    }

    @Test
    @DisplayName("CP-RF20-04 Un request de acceso con el código en blanco no es válido")
    void CP_RF20_04_requestDeAccesoSinCodigo() {
        var violaciones = Datos.validador().validate(new AccesoEstudianteRequest("est1@test.edu", "   "));

        assertThat(violaciones).isNotEmpty();
        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("codigo"));
    }
}
