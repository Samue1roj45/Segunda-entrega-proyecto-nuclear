package com.example.demo.politica;

import com.example.demo.model.DatosTiempoSesion;
import java.time.LocalDateTime;
/*
 #REQUISITO RF-22
 Patrón: STRATEGY (interfaz)
 Para qué sirve: define CÓMO se calcula el límite de ingreso a una sesión. Pasado ese
 instante el estudiante ya no puede iniciar su participación. Cambiar la regla =
 otra implementación; el resto del sistema no se toca.
 */
public interface PoliticaIngresoTardio {
    LocalDateTime limiteIngreso(DatosTiempoSesion datos);
}
