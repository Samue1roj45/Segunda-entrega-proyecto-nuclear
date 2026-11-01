package co.edu.humboldt.plataformacasos.autenticacion.puerto;

/**
 * Punto de integración con el Módulo 3 (RF-20): el código de autorización lo
 * genera y asocia ese módulo. El Módulo 1 solo pregunta si es válido, sin
 * conocer cómo se almacena, para no acoplar los dos módulos.
 */
public interface VerificadorCodigoAutorizacion {

    boolean esValido(Long estudianteId, String codigoAutorizacion);
}
