package co.edu.humboldt.plataformacasos.sesiones.soporte;

import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoProgramable;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Autorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import co.edu.humboldt.plataformacasos.sesiones.dto.ProgresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /** Sesión guardada (id 1, caso 1, docente 2) en el estado indicado. */
    public static Practica practica(String estado) {
        return new Practica(1L, 1L, 2L, "Física Nuclear I", AHORA, AHORA.plusHours(2), 60, null,
                "Laboratorio 3", null, null, estado, null, null, null);
    }

    /** Caso publicado del módulo 2 creado por el docente indicado, con 90 minutos estimados. */
    public static CasoProgramable casoPublicado(long idDocenteCreador) {
        return new CasoProgramable(1L, "Caso de prueba", 90, idDocenteCreador, EstadoCaso.PUBLICADO, true, AHORA);
    }

    public static Autorizacion autorizacion(long id, long idEstudiante, String estado) {
        return new Autorizacion(id, 1L, idEstudiante, "hash", estado, AHORA);
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
}
