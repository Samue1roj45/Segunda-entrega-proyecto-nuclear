package com.example.demo.modulo3.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.demo.model.SesionProgramada;
import com.example.demo.test.Datos;
import com.example.demo.service.validacion.ValidadorEspacioAcademico;
import com.example.demo.service.validacion.ValidadorEstudiantes;
import com.example.demo.service.validacion.ValidadorFechas;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MÓDULO 3 · ESCENARIOS POSITIVOS · RF-04 y RF-18 4
// Patrones probados: Builder (SesionProgramada) y Chain of Responsibility (validadores de la agenda).
class AgendaValidacionPositivaTest {

    @Test
    @DisplayName("CP-RF04-01 Construir la sesión programada con todos los datos")
    void CP_RF04_01_construyeSesionConTodosLosDatos() {
        SesionProgramada s = Datos.sesionBase().lugar("Laboratorio 3").mensajePersonalizado("Lleven calculadora").build();

        assertThat(s.idCaso()).isEqualTo(1L);
        assertThat(s.espacioAcademico()).isEqualTo("Física Nuclear I");
        assertThat(s.lugar()).isEqualTo("Laboratorio 3");
        assertThat(s.idsEstudiantes()).containsExactly(3L, 4L);
    }

    @Test
    @DisplayName("CP-RF04-03 Aceptar un espacio académico con nombre")
    void CP_RF04_03_aceptaEspacioAcademicoConNombre() {
        assertThatCode(() -> new ValidadorEspacioAcademico().validar(Datos.sesionBase().build()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-RF04-05 Aceptar dos estudiantes distintos")
    void CP_RF04_05_aceptaEstudiantesDistintos() {
        SesionProgramada s = Datos.sesionBase().idsEstudiantes(List.of(3L, 4L)).build();

        assertThatCode(() -> new ValidadorEstudiantes().validar(s)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-RF18-01 Aceptar una fecha de fin posterior a la de inicio")
    void CP_RF18_01_aceptaFechasCoherentes() {
        SesionProgramada s = Datos.sesionBase()
                .fechaInicio(Datos.AHORA.plusHours(1)).fechaFin(Datos.AHORA.plusHours(3)).build();

        assertThatCode(() -> new ValidadorFechas().validar(s)).doesNotThrowAnyException();
    }
}
