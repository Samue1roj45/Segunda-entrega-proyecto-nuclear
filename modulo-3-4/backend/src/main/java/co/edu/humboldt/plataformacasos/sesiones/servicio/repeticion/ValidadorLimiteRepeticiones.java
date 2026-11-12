package co.edu.humboldt.plataformacasos.sesiones.servicio.repeticion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SolicitudRepeticion;

/*
 #REQUISITO RF-25
 Para qué sirve: la repetición es EXCEPCIONAL: se concede una sola vez (máximo dos intentos por
 estudiante, regla que comparte el módulo 5).
 */
public class ValidadorLimiteRepeticiones extends ValidadorRepeticion {
    @Override
    protected void comprobar(SolicitudRepeticion s) {
        DatosRepeticion d = s.datos();
        if (d.repeticionesAutorizadas() >= DatosRepeticion.MAX_REPETICIONES) {
            throw new ReglaNegocioException("El estudiante ya tiene una repetición autorizada (máximo dos intentos)");
        }
    }
}
