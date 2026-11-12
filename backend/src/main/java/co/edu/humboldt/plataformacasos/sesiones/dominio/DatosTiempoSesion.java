package co.edu.humboldt.plataformacasos.sesiones.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/*
 #REQUISITO RF-22
 Para qué sirve: datos de tiempo de la sesión de una autorización, con la hora actual del reloj único
 de la aplicación en `ahora` (así las pruebas pueden fijar la hora).
 */
public record DatosTiempoSesion(LocalDateTime fechaInicio, int tiempoMaximoMinutos,
                                BigDecimal porcentajeLimite, String estadoSesion,
                                String estadoAutorizacion, LocalDateTime ahora) {
}
