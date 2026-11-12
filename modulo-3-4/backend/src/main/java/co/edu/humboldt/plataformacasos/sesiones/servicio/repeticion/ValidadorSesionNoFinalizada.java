package co.edu.humboldt.plataformacasos.sesiones.servicio.repeticion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SolicitudRepeticion;
/*
 #REQUISITO RF-25
 Para qué sirve: no se autoriza una repetición en una sesión ya finalizada.
 */
public class ValidadorSesionNoFinalizada extends ValidadorRepeticion {
    @Override
    protected void comprobar(SolicitudRepeticion s) {
        if ("FINALIZADO".equals(s.datos().estadoSesion())) {
            throw new ReglaNegocioException("No se puede autorizar una repetición en una sesión finalizada");
        }
    }
}
