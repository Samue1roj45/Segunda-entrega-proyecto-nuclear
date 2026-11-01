package co.edu.humboldt.plataformacasos.autenticacion.dominio;

/** Mensajes que ve el usuario al autenticarse. Un solo lugar para que pruebas y código coincidan. */
public final class MensajesAutenticacion {

    /** Genérico a propósito: no revela si el correo existe ni cuál dato falló. */
    public static final String CREDENCIALES_INVALIDAS = "Correo o código de autorización inválidos.";
    public static final String CORREO_OBLIGATORIO = "El correo institucional es obligatorio.";
    public static final String CODIGO_OBLIGATORIO = "El código de autorización es obligatorio.";
    public static final String CAMPOS_OBLIGATORIOS = "El correo institucional y el código de autorización son obligatorios.";
    public static final String CAMPO_DEMASIADO_LARGO = "El correo o el código superan la longitud permitida.";

    private MensajesAutenticacion() {
    }

    public static String correoNoInstitucional(String dominio) {
        return "Debe ingresar su correo institucional (@" + dominio + ").";
    }
}
