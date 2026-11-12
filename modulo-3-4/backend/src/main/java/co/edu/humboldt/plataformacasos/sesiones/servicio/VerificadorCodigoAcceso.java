package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.autenticacion.puerto.VerificadorCodigoAutorizacion;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.AutorizacionRepositorio;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 #REQUISITO RF-20 (puerto del módulo 1, RF-01)
 Para qué sirve: implementación oficial de VerificadorCodigoAutorizacion. El módulo 1 pregunta si el
 código es válido para el estudiante sin saber cómo se guarda: es válido si coincide con el de alguna
 autorización ACTIVA del estudiante en una sesión que todavía no ha finalizado.
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
        return autorizaciones.hashesVigentes(estudianteId).stream()
                .anyMatch(hash -> codificador.matches(codigoAutorizacion, hash));
    }
}
