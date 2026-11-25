package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Eslabón final de la cadena de eliminación: una cuenta con historial académico o de comunicaciones no se
 * borra, porque varias de esas tablas no tienen llave hacia {@code usuario} y quedarían registros huérfanos.
 */
public class ValidadorSinRegistrosHistoricos extends ValidadorBase<SolicitudCambioCuenta> {

    private final RegistrosHistoricosUsuario registros;

    public ValidadorSinRegistrosHistoricos(RegistrosHistoricosUsuario registros) {
        this.registros = registros;
    }

    @Override
    protected ResultadoValidacion validarCondicion(SolicitudCambioCuenta solicitud) {
        List<RegistroHistorico> encontrados = registros.de(solicitud.cuenta().getId());
        if (encontrados.isEmpty()) {
            return ResultadoValidacion.exito();
        }
        String resumen = encontrados.stream()
                .map(r -> r.descripcion() + " (" + r.cantidad() + ")")
                .collect(Collectors.joining(", "));
        return ResultadoValidacion.rechazo(MensajesAdministracion.CON_HISTORIAL + " " + resumen + ". "
                + MensajesAdministracion.SUGERENCIA_ARCHIVAR);
    }
}
