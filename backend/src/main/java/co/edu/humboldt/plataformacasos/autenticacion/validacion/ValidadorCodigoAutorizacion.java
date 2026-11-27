package co.edu.humboldt.plataformacasos.autenticacion.validacion;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.IntentoAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.puerto.VerificadorCodigoAutorizacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ValidadorBase;

import java.util.Objects;

// Patrón Chain of Responsibility: eslabón que verifica el código de acceso del estudiante a través del puerto VerificadorCodigoAutorizacion.
/**
 * Eslabón 4 (RF-01): el código de autorización es válido para ese estudiante.
 * La regla de validez la decide el Módulo 3 (RF-20) a través de {@link VerificadorCodigoAutorizacion}.
 */
public class ValidadorCodigoAutorizacion extends ValidadorBase<IntentoAutenticacion> {

    private final VerificadorCodigoAutorizacion verificador;

    public ValidadorCodigoAutorizacion(VerificadorCodigoAutorizacion verificador) {
        this.verificador = Objects.requireNonNull(verificador, "verificador");
    }

    @Override
    protected ResultadoValidacion validarCondicion(IntentoAutenticacion intento) {
        Long estudianteId = intento.usuario().map(u -> u.getId()).orElse(null);
        // Sin pasar a mayúsculas: la contraseña distingue mayúsculas. El verificador normaliza solo el código de sesión.
        String codigo = intento.codigoAutorizacion().trim();
        return estudianteId != null && verificador.esValido(estudianteId, codigo)
                ? ResultadoValidacion.exito()
                : ResultadoValidacion.rechazo(MensajesAutenticacion.CREDENCIALES_INVALIDAS);
    }
}
