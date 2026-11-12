package co.edu.humboldt.plataformacasos.sesiones.evento;

/*
 #REQUISITO RF-19 (lo escucha el módulo 5 para RF-24)
 Patrón: OBSERVER (evento)
 Para qué sirve: la sesión pasó a FINALIZADO (manual o automáticamente). El módulo de evaluación
 cierra los intentos que seguían en curso como INCOMPLETOS, registra las ausencias y califica.
 Se atiende dentro de la misma transacción: si la evaluación falla, la sesión no queda finalizada.
 */
public record SesionFinalizadaEvento(Long idPractica, String tipoCierre) {
}
