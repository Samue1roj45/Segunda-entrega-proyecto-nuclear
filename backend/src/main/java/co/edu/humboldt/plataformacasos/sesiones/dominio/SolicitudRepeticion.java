package co.edu.humboldt.plataformacasos.sesiones.dominio;
/*
 #REQUISITO RF-25
 Patrón: CHAIN OF RESPONSIBILITY (mensaje que recorre la cadena)
 Para qué sirve: agrupa la petición del docente con los datos ya cargados, para que cada
 validador revise su regla sin volver a consultar la base de datos.
 */
public record SolicitudRepeticion(Long idAutorizacion, Long idDocente, String motivo, DatosRepeticion datos) {
}
