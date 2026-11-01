package co.edu.humboldt.plataformacasos.autenticacion.dominio;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;

import java.util.Optional;

/**
 * Intento ya con formato válido: el usuario encontrado por el correo (si existe) y el código.
 * Se busca el usuario una sola vez y lo comparten los validadores de credenciales.
 */
public record IntentoAutenticacion(Optional<Usuario> usuario, String codigoAutorizacion) {

    public IntentoAutenticacion {
        usuario = usuario == null ? Optional.empty() : usuario;
    }
}
