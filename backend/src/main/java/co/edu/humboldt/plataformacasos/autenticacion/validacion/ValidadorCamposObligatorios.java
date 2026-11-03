package co.edu.humboldt.plataformacasos.autenticacion.validacion;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;

/** Eslabón 1 (RF-01): correo y código presentes y con longitud razonable. */
public class ValidadorCamposObligatorios extends ValidadorBase<SolicitudAutenticacion> {

    static final int LONGITUD_MAXIMA_CORREO = 150;
    static final int LONGITUD_MAXIMA_CODIGO = 50;

    @Override
    protected ResultadoValidacion validarCondicion(SolicitudAutenticacion solicitud) {
        boolean sinCorreo = estaVacio(solicitud.correoInstitucional());
        boolean sinCodigo = estaVacio(solicitud.codigoAutorizacion());
        if (sinCorreo && sinCodigo) {
            return ResultadoValidacion.rechazo(MensajesAutenticacion.CAMPOS_OBLIGATORIOS);
        }
        if (sinCorreo) {
            return ResultadoValidacion.rechazo(MensajesAutenticacion.CORREO_OBLIGATORIO);
        }
        if (sinCodigo) {
            return ResultadoValidacion.rechazo(MensajesAutenticacion.CODIGO_OBLIGATORIO);
        }
        if (solicitud.correoInstitucional().trim().length() > LONGITUD_MAXIMA_CORREO
                || solicitud.codigoAutorizacion().trim().length() > LONGITUD_MAXIMA_CODIGO) {
            return ResultadoValidacion.rechazo(MensajesAutenticacion.CAMPO_DEMASIADO_LARGO);
        }
        return ResultadoValidacion.exito();
    }

    private static boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
