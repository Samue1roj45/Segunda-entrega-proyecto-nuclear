package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.compartido.validacion.Validador;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.springframework.stereotype.Component;

// Patrón Chain of Responsibility: aquí se ensamblan las cadenas que protegen el archivo y la eliminación de cuentas.
/**
 * Cada cadena tiene sus propios eslabones (un eslabón guarda a su siguiente, así que no se comparten).
 * Agregar una regla es sumar un eslabón aquí, sin tocar el servicio.
 */
@Component
public class CadenasAdministracionCuentas {

    private final Validador<SolicitudCambioCuenta> archivo;
    private final Validador<SolicitudCambioCuenta> eliminacion;

    public CadenasAdministracionCuentas(UsuarioRepositorio usuarios, RegistrosHistoricosUsuario registros) {
        this.archivo = new ValidadorCuentaPropia();
        this.archivo.establecerSiguiente(new ValidadorAdministradorRestante(usuarios));

        this.eliminacion = new ValidadorCuentaPropia();
        this.eliminacion.establecerSiguiente(new ValidadorAdministradorRestante(usuarios))
                .establecerSiguiente(new ValidadorSinRegistrosHistoricos(registros));
    }

    /** Cuenta propia → último administrador. */
    public Validador<SolicitudCambioCuenta> archivo() {
        return archivo;
    }

    /** Cuenta propia → último administrador → historial académico y de comunicaciones. */
    public Validador<SolicitudCambioCuenta> eliminacion() {
        return eliminacion;
    }
}
