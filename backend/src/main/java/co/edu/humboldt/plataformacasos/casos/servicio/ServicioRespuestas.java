package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosRespuestaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.JustificacionDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.JustificacionRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.MarcarCorrectaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.PreguntaDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RespuestaDTO;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioRespuestas implements GestionRespuestas {

    private final CasoEstudioRepositorio casos;
    private final AccesoCasos acceso;

    public ServicioRespuestas(CasoEstudioRepositorio casos, AccesoCasos acceso) {
        this.casos = casos;
        this.acceso = acceso;
    }

    @Override
    public RespuestaDTO agregar(Long idCaso, Long idPregunta, DatosRespuestaRequest datos) {
        Respuesta respuesta = acceso.paraModificar(idCaso).pregunta(idPregunta)
                .agregarRespuesta(datos.codigo(), datos.texto(), datos.esCorrecta(), datos.porcentaje(),
                        datos.justificacion());
        casos.flush();
        return RespuestaDTO.de(respuesta);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaDTO consultar(Long idCaso, Long idRespuesta) {
        return RespuestaDTO.de(acceso.paraConsultar(idCaso).respuesta(idRespuesta));
    }

    @Override
    public RespuestaDTO actualizar(Long idCaso, Long idRespuesta, DatosRespuestaRequest datos) {
        Respuesta respuesta = acceso.paraModificar(idCaso).respuesta(idRespuesta);
        respuesta.actualizar(datos.codigo(), datos.texto(), datos.esCorrecta(), datos.porcentaje(),
                datos.justificacion());
        return RespuestaDTO.de(respuesta);
    }

    @Override
    public void eliminar(Long idCaso, Long idRespuesta) {
        Respuesta respuesta = acceso.paraModificar(idCaso).respuesta(idRespuesta);
        respuesta.getPregunta().eliminarRespuesta(idRespuesta);
    }

    @Override
    public PreguntaDTO marcarCorrecta(Long idCaso, Long idPregunta, MarcarCorrectaRequest solicitud) {
        if (solicitud.idRespuesta() == null) {
            throw new SolicitudInvalidaException("Debe indicar la respuesta correcta.");
        }
        Pregunta pregunta = acceso.paraModificar(idCaso).pregunta(idPregunta);
        pregunta.marcarCorrecta(solicitud.idRespuesta());
        return PreguntaDTO.de(pregunta);
    }

    @Override
    @Transactional(readOnly = true)
    public JustificacionDTO consultarJustificacion(Long idCaso, Long idRespuesta) {
        return JustificacionDTO.de(acceso.paraConsultar(idCaso).respuesta(idRespuesta));
    }

    @Override
    public JustificacionDTO registrarJustificacion(Long idCaso, Long idRespuesta, JustificacionRequest solicitud) {
        Respuesta respuesta = acceso.paraModificar(idCaso).respuesta(idRespuesta);
        respuesta.justificar(solicitud.justificacion());
        return JustificacionDTO.de(respuesta);
    }
}
