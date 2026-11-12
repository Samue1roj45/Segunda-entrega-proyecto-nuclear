/**
 * Adaptadores entre módulos (composición de la aplicación). Cada módulo declara los puertos que necesita
 * y aquí se conectan con los módulos que los cumplen, sin que un módulo conozca las clases internas del otro:
 * <ul>
 *   <li>{@link co.edu.humboldt.plataformacasos.integracion.ActorDesdeSesion}: módulo 1 → módulos 5 y 6.</li>
 *   <li>{@link co.edu.humboldt.plataformacasos.integracion.CatalogoAcademicoIntegrado}: módulos 1, 2 y 3 →
 *       módulos 5 y 6.</li>
 *   <li>{@link co.edu.humboldt.plataformacasos.integracion.PoliticaInicioDesdeSesiones}: módulo 4 (RF-22) →
 *       módulo 5 (RF-12).</li>
 *   <li>{@link co.edu.humboldt.plataformacasos.integracion.ObservadorSesiones}: eventos del módulo 3 →
 *       evaluación (RF-24) y notificaciones (RF-21).</li>
 * </ul>
 * El módulo 3 implementa directamente el puerto del módulo 1 (VerificadorCodigoAcceso, RF-20) y usa
 * los contratos del módulo 2 (integracion.ConsultaCasosProgramables y ContenidoCasoInterno).
 */
package co.edu.humboldt.plataformacasos.integracion;
