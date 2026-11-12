package co.edu.humboldt.plataformacasos.sesiones.servicio.repeticion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SolicitudRepeticion;
/*
 #REQUISITO RF-25
 Para qué sirve: solo el docente CREADOR del caso puede autorizar una repetición.
 */
public class ValidadorDocenteCreador extends ValidadorRepeticion {
    @Override
    protected void comprobar(SolicitudRepeticion s) {
        if (!s.idDocente().equals(s.datos().idDocenteCreador())) {
            throw new AccesoDenegadoException("Solo el docente creador del caso puede autorizar una repetición");
        }
    }
}
