package co.edu.humboldt.plataformacasos.sesiones.tareas;

import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioGestionSesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 #REQUISITO RF-19
 Para qué sirve: cierre AUTOMÁTICO de sesiones. Cada minuto finaliza las sesiones EN_CURSO cuyo
 plazo ya pasó; al finalizar, el módulo 5 cierra los intentos abiertos, marca ausentes y califica.
 */
@Component
public class TareaCierreAutomatico {

    private static final Logger log = LoggerFactory.getLogger(TareaCierreAutomatico.class);

    private final ServicioGestionSesion gestion;

    public TareaCierreAutomatico(ServicioGestionSesion gestion) {
        this.gestion = gestion;
    }

    @Scheduled(fixedDelayString = "${app.sesiones.intervalo-cierre-ms:60000}")
    public void cerrarVencidas() {
        try {
            int cerradas = gestion.cerrarVencidas();
            if (cerradas > 0) {
                log.info("Cierre automático: {} sesión(es) finalizada(s)", cerradas);
            }
        } catch (Exception e) {
            log.error("Error en el cierre automático de sesiones", e);
        }
    }
}
