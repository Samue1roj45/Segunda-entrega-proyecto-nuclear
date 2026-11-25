package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.autenticacion.puerto.VerificadorCodigoAutorizacion;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.AutorizacionRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Patrón Adapter (Ports & Adapters): implementa el puerto VerificadorCodigoAutorizacion de M1 comparando el código con el hash BCrypt de la autorización.
/*
 #REQUISITO RF-20 (puerto del módulo 1, RF-01)
 Para qué sirve: implementación oficial de VerificadorCodigoAutorizacion. El módulo 1 pregunta si el
 código es válido para el estudiante sin saber cómo se guarda: es válido si coincide con el de alguna
 autorización del estudiante. El código es su contraseña: sirve aunque lo hayan retirado de una sesión o la
 sesión haya finalizado (retirar no lo saca de la plataforma).
 */
@Component
public class VerificadorCodigoAcceso implements VerificadorCodigoAutorizacion {

    private final AutorizacionRepositorio autorizaciones;
    private final PasswordEncoder codificador;

    public VerificadorCodigoAcceso(AutorizacionRepositorio autorizaciones, PasswordEncoder codificador) {
        this.autorizaciones = autorizaciones;
        this.codificador = codificador;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean esValido(Long estudianteId, String codigoAutorizacion) {
        return autorizaciones.hashesDeEstudiante(estudianteId).stream()
                .anyMatch(hash -> codificador.matches(codigoAutorizacion, hash));
    }
}
