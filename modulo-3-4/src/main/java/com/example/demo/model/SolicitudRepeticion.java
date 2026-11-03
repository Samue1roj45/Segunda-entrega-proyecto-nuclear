package com.example.demo.model;
/*
 #REQUISITO RF-25
 Patrón: CHAIN OF RESPONSIBILITY (mensaje que recorre la cadena)
 Para qué sirve: agrupa la petición del docente con los datos ya cargados, para que cada
 validador revise su regla sin volver a consultar la BD.
 */
public record SolicitudRepeticion(Long idAutorizacion, Long idDocente, String motivo, DatosRepeticion datos) {
}
