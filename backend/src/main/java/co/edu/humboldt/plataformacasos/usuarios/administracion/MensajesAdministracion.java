package co.edu.humboldt.plataformacasos.usuarios.administracion;

/** Mensajes de la administración de docentes (RF-03) y de cuentas. */
public final class MensajesAdministracion {

    public static final String NOMBRE_OBLIGATORIO = "El nombre completo es obligatorio.";
    public static final String CORREO_OBLIGATORIO = "El correo es obligatorio.";
    public static final String CORREO_INVALIDO = "El correo no tiene un formato válido.";
    public static final String CORREO_DUPLICADO = "El correo ya está registrado.";
    public static final String PERMISOS_OBLIGATORIOS = "Debe enviar la lista de permisos (puede estar vacía).";
    public static final String DOCENTE_NO_ENCONTRADO = "No existe un docente con ese identificador.";

    public static final String USUARIO_NO_ENCONTRADO = "No existe un usuario con ese identificador.";
    public static final String YA_ARCHIVADA = "La cuenta ya está archivada.";
    public static final String YA_ACTIVA = "La cuenta ya está activa.";
    public static final String CUENTA_PROPIA = "No puedes archivar ni eliminar tu propia cuenta.";
    public static final String ULTIMO_ADMINISTRADOR = "Es el último administrador activo: el sistema no puede quedar sin administrador.";
    public static final String CON_HISTORIAL = "No se puede eliminar definitivamente porque la cuenta tiene registros que deben conservarse:";
    public static final String SUGERENCIA_ARCHIVAR = "Mantén la cuenta archivada para conservar su historial.";
    public static final String REGISTROS_CONCURRENTES = "No se puede eliminar: la cuenta tiene registros asociados. Mantenla archivada.";

    private MensajesAdministracion() {
    }
}
