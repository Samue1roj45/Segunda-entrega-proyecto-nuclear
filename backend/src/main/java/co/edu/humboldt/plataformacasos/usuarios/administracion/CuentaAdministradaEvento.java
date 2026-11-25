package co.edu.humboldt.plataformacasos.usuarios.administracion;

/**
 * Se publica cuando el administrador archiva, reactiva o elimina una cuenta. Lleva todo lo que necesita
 * el registro de auditoría: quién, qué cuenta y el resultado. {@code idCuentaAvisada} es opcional: la cuenta
 * que recibe un aviso propio (por ejemplo, al reactivarla).
 */
public record CuentaAdministradaEvento(String accion, Long actorId, String actorCorreo, String detalle,
                                       Long idCuentaAvisada, String tituloAviso, String mensajeAviso) {

    public static CuentaAdministradaEvento sinAvisoPropio(String accion, Long actorId, String actorCorreo, String detalle) {
        return new CuentaAdministradaEvento(accion, actorId, actorCorreo, detalle, null, null, null);
    }
}
