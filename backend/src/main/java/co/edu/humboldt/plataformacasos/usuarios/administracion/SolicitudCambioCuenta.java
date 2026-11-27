package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;

import java.util.Objects;

/** Lo que validan las cadenas de archivo y de eliminación: la cuenta afectada y quién hace el cambio. */
public record SolicitudCambioCuenta(Usuario cuenta, Long idActor) {

    public SolicitudCambioCuenta {
        Objects.requireNonNull(cuenta, "cuenta");
        Objects.requireNonNull(idActor, "idActor");
    }
}
