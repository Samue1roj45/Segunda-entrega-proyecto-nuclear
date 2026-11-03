package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosPreguntaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.PreguntaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioPreguntas implements GestionPreguntas {

    private final CasoEstudioRepositorio casos;
    private final AccesoCasos acceso;

    public ServicioPreguntas(CasoEstudioRepositorio casos, AccesoCasos acceso) {
        this.casos = casos;
        this.acceso = acceso;
    }

    @Override
    public PreguntaDTO agregar(Long idCaso, Long idEscenario, DatosPreguntaRequest datos) {
        Pregunta pregunta = acceso.paraModificar(idCaso).escenario(idEscenario).agregarPregunta(datos.enunciado());
        casos.flush();
        return PreguntaDTO.de(pregunta);
    }

    @Override
    @Transactional(readOnly = true)
    public PreguntaDTO consultar(Long idCaso, Long idPregunta) {
        return PreguntaDTO.de(acceso.paraConsultar(idCaso).pregunta(idPregunta));
    }

    @Override
    public PreguntaDTO actualizar(Long idCaso, Long idPregunta, DatosPreguntaRequest datos) {
        Pregunta pregunta = acceso.paraModificar(idCaso).pregunta(idPregunta);
        pregunta.actualizarEnunciado(datos.enunciado());
        return PreguntaDTO.de(pregunta);
    }

    @Override
    public void eliminar(Long idCaso, Long idPregunta) {
        Pregunta pregunta = acceso.paraModificar(idCaso).pregunta(idPregunta);
        pregunta.getEscenario().eliminarPregunta(idPregunta);
    }
}
