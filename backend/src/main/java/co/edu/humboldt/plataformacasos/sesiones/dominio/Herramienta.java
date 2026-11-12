package co.edu.humboldt.plataformacasos.sesiones.dominio;

/*
 #REQUISITO RF-28
 Para qué sirve: herramienta profesional disponible en un caso (calculadora, tabla de referencia,
 documento, enlace...). "contenido" es la definición que usa el frontend (JSON o URL).
 */
public record Herramienta(Long id, Long idCaso, String nombre, TipoHerramienta tipo, String descripcion,
                          String contenido, int orden) {
}
