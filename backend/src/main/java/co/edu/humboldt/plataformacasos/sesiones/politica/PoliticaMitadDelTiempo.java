package co.edu.humboldt.plataformacasos.sesiones.politica;

import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosTiempoSesion;
import java.time.LocalDateTime;
/*
 #REQUISITO RF-22
 Patrón: STRATEGY (estrategia concreta, la POR DEFECTO)
 Regla: se puede ingresar hasta que pase la MITAD del tiempo máximo, contada desde la
 fecha de inicio de la sesión (el inicio real si el docente la inició). Es la única regla de
 ingreso tardío del sistema: el módulo 5 la consulta antes de registrar un intento nuevo.
 */
public class PoliticaMitadDelTiempo implements PoliticaIngresoTardio {
    @Override
    public LocalDateTime limiteIngreso(DatosTiempoSesion d) {
        return d.fechaInicio().plusMinutes(d.tiempoMaximoMinutos() / 2);
    }
}
