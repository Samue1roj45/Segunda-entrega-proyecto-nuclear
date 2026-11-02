package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosRespuestaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.MarcarCorrectaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.PreguntaDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RespuestaDTO;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

/** RF-07: opciones de respuesta de cada pregunta. */
public interface GestionRespuestas {

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    RespuestaDTO agregar(Long idCaso, Long idPregunta, DatosRespuestaRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    RespuestaDTO consultar(Long idCaso, Long idRespuesta);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    RespuestaDTO actualizar(Long idCaso, Long idRespuesta, DatosRespuestaRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    void eliminar(Long idCaso, Long idRespuesta);

    /** Deja una sola opción correcta en la pregunta, dentro de una misma transacción. */
    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    PreguntaDTO marcarCorrecta(Long idCaso, Long idPregunta, MarcarCorrectaRequest solicitud);
}
