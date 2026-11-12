package co.edu.humboldt.plataformacasos.sesiones.evento;

import java.time.LocalDateTime;

/*
 #REQUISITO RF-18 (lo escucha el módulo 6 para RF-21)
 Patrón: OBSERVER (evento)
 Para qué sirve: el docente cambió las condiciones de una sesión (fechas, tiempo, lugar o mensaje);
 los estudiantes convocados deben recibir el aviso de cambio.
 */
public record SesionModificadaEvento(Long idPractica, String motivo, LocalDateTime fecha) {
}
