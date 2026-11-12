package co.edu.humboldt.plataformacasos.evaluacion.contrato;

/**
 * Puerto hacia la regla de ingreso del módulo 4 (RF-22): antes de registrar el inicio de un intento
 * NUEVO (RF-12) se pregunta si el estudiante todavía puede ingresar (sesión en curso, autorización
 * activa y dentro del límite de ingreso tardío). Retomar un intento EN_CURSO no lo consulta.
 */
public interface PoliticaInicioIntento {

    /** Lanza ReglaNegocioException con el motivo si el ingreso no está permitido. */
    void exigirInicioPermitido(Long autorizacionId);
}
