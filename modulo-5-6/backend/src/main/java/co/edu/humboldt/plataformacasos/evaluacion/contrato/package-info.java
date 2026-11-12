/**
 * Puertos de los módulos 5 y 6 hacia los demás módulos. Este módulo no conoce las clases de los otros:
 * declara aquí lo que necesita y la aplicación (carpeta aplicacion/, paquete integracion) lo conecta:
 * <ul>
 *   <li>{@link co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActualResolver}: el usuario del
 *       token del módulo 1, con su rol y sus permisos.</li>
 *   <li>{@link co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico}: sesiones y
 *       autorizaciones (módulo 3), estudiantes (módulo 1) y preguntas, opciones y rúbrica (módulo 2).</li>
 *   <li>{@link co.edu.humboldt.plataformacasos.evaluacion.contrato.PoliticaInicioIntento}: la regla de
 *       ingreso tardío (RF-22, módulo 4).</li>
 * </ul>
 * Las pruebas del módulo usan dobles en memoria de estos puertos.
 */
package co.edu.humboldt.plataformacasos.evaluacion.contrato;
