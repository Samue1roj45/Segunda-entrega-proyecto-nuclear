package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Puerto que este módulo (RF-12, dueño de la participación) ofrece al módulo 4 (RF-10, RF-11, RF-22,
 * RF-23, RF-25): consultar los intentos, guardar o cambiar una respuesta y cerrar un intento por tiempo.
 * No verifica quién llama: el módulo 4 comprueba antes, con su Proxy, que el intento sea del estudiante.
 */
public interface RegistroParticipaciones {

    Optional<IntentoParticipacion> intento(Long participacionId);

    List<IntentoParticipacion> intentosDeAutorizacion(Long autorizacionId);

    List<IntentoParticipacion> intentosEnCurso();

    /** Respuestas elegidas en el intento: id de pregunta → id de opción. */
    Map<Long, Long> respuestasSeleccionadas(Long participacionId);

    /** RF-10/RF-11: guarda la opción elegida o la cambia si la pregunta ya tenía respuesta. */
    void registrarRespuesta(Long participacionId, Long preguntaId, Long respuestaId);

    /**
     * RF-23: el tiempo máximo se agotó. El intento se cierra como FINALIZADA si respondió todo o como
     * INCOMPLETA si faltaron preguntas (RF-24), y se califica con la rúbrica (RF-13).
     */
    IntentoParticipacion cerrarPorTiempo(Long participacionId);
}
