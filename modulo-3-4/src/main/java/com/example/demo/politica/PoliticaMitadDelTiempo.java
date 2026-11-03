package com.example.demo.politica;

import com.example.demo.model.DatosTiempoSesion;
import java.time.LocalDateTime;
/*
 #REQUISITO RF-22
 Patrón: STRATEGY (estrategia concreta, la POR DEFECTO)
 Regla: se puede ingresar hasta que pase la MITAD del tiempo máximo, contada desde la
 fecha de inicio de la sesión. Es exactamente la regla de fn_ingreso_tardio en la BD, así
 que Java y BD siempre coinciden.
 */
public class PoliticaMitadDelTiempo implements PoliticaIngresoTardio {
    @Override
    public LocalDateTime limiteIngreso(DatosTiempoSesion d) {
        return d.fechaInicio().plusMinutes(d.tiempoMaximoMinutos() / 2);
    }
}
