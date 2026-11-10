package com.example.demo.test;

import com.example.demo.dto.ProgresoDto;
import com.example.demo.dto.SesionDto;
import com.example.demo.model.DatosRepeticion;
import com.example.demo.model.SesionProgramada;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.UncategorizedSQLException;

// Datos de apoyo para las pruebas unitarias (compartido por módulo 3 y módulo 4).
// Todos los valores son ficticios: no hay credenciales reales.
public final class Datos {

    public static final LocalDateTime AHORA = LocalDateTime.now();

    private Datos() { }

    /** Sesión programada válida; cada prueba cambia solo lo que necesita. */
    public static SesionProgramada.Builder sesionBase() {
        return SesionProgramada.builder().idCaso(1L).idDocente(2L).espacioAcademico("Física Nuclear I")
                .fechaInicio(AHORA.plusHours(1)).fechaFin(AHORA.plusHours(3)).tiempoMaximoMinutos(60)
                .idsEstudiantes(List.of(3L, 4L));
    }

    public static SesionDto sesionDto(String estado) {
        return new SesionDto(1L, 1L, "Caso de prueba", "Física Nuclear I", AHORA, AHORA.plusHours(2), 60,
                "Laboratorio 3", null, null, estado, null, null, null, List.of());
    }

    public static ProgresoDto progreso(int respondidas, int total) {
        BigDecimal pct = BigDecimal.valueOf(100.0 * respondidas / total).setScale(2, java.math.RoundingMode.HALF_UP);
        return new ProgresoDto(7L, total, respondidas, pct);
    }

    public static DatosRepeticion datosRepeticion(String estadoAut, long creador, long docenteSesion,
                                                  String estadoSesion, int usados, int repeticiones) {
        return new DatosRepeticion(estadoAut, creador, docenteSesion, estadoSesion, usados, repeticiones);
    }

    public static Validator validador() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    /** Error que lanza la BD con SIGNAL SQLSTATE '45000' (regla de negocio de un trigger o procedimiento). */
    public static UncategorizedSQLException errorBd45000(String mensaje) {
        return new UncategorizedSQLException("CallableStatementCallback", "CALL sp_prueba()",
                new SQLException(mensaje, "45000"));
    }
}
