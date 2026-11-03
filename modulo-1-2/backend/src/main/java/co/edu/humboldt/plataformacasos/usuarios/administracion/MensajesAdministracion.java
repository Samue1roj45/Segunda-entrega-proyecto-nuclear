package co.edu.humboldt.plataformacasos.usuarios.administracion;

/** Mensajes de la administración de docentes (RF-03). */
public final class MensajesAdministracion {

    public static final String NOMBRE_OBLIGATORIO = "El nombre completo es obligatorio.";
    public static final String CORREO_OBLIGATORIO = "El correo es obligatorio.";
    public static final String CORREO_INVALIDO = "El correo no tiene un formato válido.";
    public static final String CORREO_DUPLICADO = "El correo ya está registrado.";
    public static final String PERMISOS_OBLIGATORIOS = "Debe enviar la lista de permisos (puede estar vacía).";
    public static final String DOCENTE_NO_ENCONTRADO = "No existe un docente con ese identificador.";

    private MensajesAdministracion() {
    }
}
