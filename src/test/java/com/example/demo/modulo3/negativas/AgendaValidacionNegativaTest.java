package com.example.demo.modulo3.negativas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.SesionProgramada;
import com.example.demo.test.Datos;
import com.example.demo.service.validacion.ValidadorEspacioAcademico;
import com.example.demo.service.validacion.ValidadorEstudiantes;
import com.example.demo.service.validacion.ValidadorFechas;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MÓDULO 3 · ESCENARIOS NEGATIVOS · RF-04 y RF-18 4
// Pareja exacta de AgendaValidacionPositivaTest: cada positivo tiene su negativo.
class AgendaValidacionNegativaTest {

    @Test
    @DisplayName("CP-RF04-02 Rechazar la construcción de una sesión sin caso")
    void CP_RF04_02_rechazaSesionSinCaso() {
        assertThatThrownBy(() -> SesionProgramada.builder().idDocente(2L)
                .fechaInicio(Datos.AHORA.plusHours(1)).fechaFin(Datos.AHORA.plusHours(3))
                .tiempoMaximoMinutos(60).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("idCaso");
    }

    @Test
    @DisplayName("CP-RF04-04 Rechazar un espacio académico en blanco")
    void CP_RF04_04_rechazaEspacioAcademicoVacio() {
        SesionProgramada s = Datos.sesionBase().espacioAcademico("   ").build();

        assertThatThrownBy(() -> new ValidadorEspacioAcademico().validar(s))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("espacio académico");
    }

    @Test
    @DisplayName("CP-RF04-06 Rechazar una sesión sin estudiantes")
    void CP_RF04_06_rechazaListaDeEstudiantesVacia() {
        SesionProgramada s = Datos.sesionBase().idsEstudiantes(List.of()).build();

        assertThatThrownBy(() -> new ValidadorEstudiantes().validar(s))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos un estudiante");
    }

    @Test
    @DisplayName("CP-RF18-02 Rechazar una fecha de fin anterior a la de inicio")
    void CP_RF18_02_rechazaFinAnteriorAlInicio() {
        SesionProgramada s = Datos.sesionBase()
                .fechaInicio(Datos.AHORA.plusHours(3)).fechaFin(Datos.AHORA.plusHours(1)).build();

        assertThatThrownBy(() -> new ValidadorFechas().validar(s))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("posterior");
    }
}
