package co.edu.humboldt.plataformacasos.autenticacion.google;

import java.util.Optional;

/** Comprueba un ID token de Google y devuelve la identidad que prueba, o vacío si no es válido. */
public interface VerificadorGoogle {

    record IdentidadGoogle(String correo, boolean correoVerificado) { }

    Optional<IdentidadGoogle> verificar(String credencial);
}
