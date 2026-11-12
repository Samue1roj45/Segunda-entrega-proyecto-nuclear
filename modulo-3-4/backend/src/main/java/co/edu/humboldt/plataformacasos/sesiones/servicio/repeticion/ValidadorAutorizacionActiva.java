package co.edu.humboldt.plataformacasos.sesiones.servicio.repeticion;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Autorizacion;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SolicitudRepeticion;
// Patrón Chain of Responsibility: eslabón de repetición que exige autorización ACTIVA (RF-25).
/*
 #REQUISITO RF-25
 Para qué sirve: no se concede un intento extra a un estudiante retirado de la sesión.
 */
public class ValidadorAutorizacionActiva extends ValidadorRepeticion {
    @Override
    protected void comprobar(SolicitudRepeticion s) {
        if (!Autorizacion.ACTIVA.equals(s.datos().estadoAutorizacion())) {
            throw new ReglaNegocioException("La autorización del estudiante fue retirada");
        }
    }
}
