package co.edu.humboldt.plataformacasos.invitaciones;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Entradas y salidas de las invitaciones de estudiantes. */
public final class ModeloInvitaciones {

    private ModeloInvitaciones() {
    }

    public record SolicitudInvitar(List<String> correos) {
    }

    /** Resultado de invitar a UN correo. enlace solo viaja al docente que invita (para copiarlo si el correo no llega). */
    public record ResultadoInvitacion(String correo, String resultado, String mensaje, String enlace, boolean correoEnviado) {
    }

    public record InvitacionDocente(long id, String correo, String estado, LocalDateTime creada, LocalDateTime vence,
                                    LocalDateTime aceptada, String estudiante) {
    }

    /** Lo que ve quien abre el enlace, antes de crear la cuenta. */
    public record InvitacionPublica(String correo, String docente, LocalDateTime vence) {
    }

    public record DatosRegistro(String nombreCompleto, String tipoDocumento, String numeroDocumento, LocalDate fechaNacimiento,
                                String telefono, String contrasena, Boolean aceptaTratamientoDatos) {
    }

    public record CuentaCreada(String correo, String mensaje) {
    }
}
