package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;

/** Eslabón: nadie puede archivar ni eliminar su propia cuenta desde la administración. */
public class ValidadorCuentaPropia extends ValidadorBase<SolicitudCambioCuenta> {

    @Override
    protected ResultadoValidacion validarCondicion(SolicitudCambioCuenta solicitud) {
        return solicitud.cuenta().getId().equals(solicitud.idActor())
                ? ResultadoValidacion.rechazo(MensajesAdministracion.CUENTA_PROPIA)
                : ResultadoValidacion.exito();
    }
}
