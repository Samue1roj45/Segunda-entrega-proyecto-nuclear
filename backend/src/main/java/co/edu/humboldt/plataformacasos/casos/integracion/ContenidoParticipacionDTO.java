package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.Escenario;
import co.edu.humboldt.plataformacasos.casos.dominio.Pregunta;
import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;

import java.util.List;

/**
 * Lo que el estudiante puede ver durante un intento (RF-10, RF-14). A propósito no tiene campos para
 * la opción correcta, el porcentaje ni la justificación: no se pueden filtrar porque no existen aquí.
 */
public record ContenidoParticipacionDTO(Long idCaso, String nombre, String desarrolloSituacional,
                                        String contextoHistoria, Integer tiempoEstimadoMinutos,
                                        List<EscenarioVisible> escenarios) {

    public record EscenarioVisible(Long id, int orden, String nombre, String contexto,
                                   List<PreguntaVisible> preguntas) {
    }

    public record PreguntaVisible(Long id, int orden, String enunciado, List<OpcionVisible> opciones) {
    }

    public record OpcionVisible(Long id, String codigo, String texto) {
    }

    static ContenidoParticipacionDTO de(CasoEstudio caso) {
        return new ContenidoParticipacionDTO(caso.getId(), caso.getNombre(), caso.getDesarrolloSituacional(),
                caso.getContextoHistoria(), caso.getTiempoEstimadoMinutos(),
                caso.getEscenarios().stream().map(ContenidoParticipacionDTO::escenario).toList());
    }

    private static EscenarioVisible escenario(Escenario escenario) {
        return new EscenarioVisible(escenario.getId(), escenario.getOrden(), escenario.getNombre(),
                escenario.getContexto(), escenario.getPreguntas().stream()
                .map(ContenidoParticipacionDTO::pregunta).toList());
    }

    private static PreguntaVisible pregunta(Pregunta pregunta) {
        return new PreguntaVisible(pregunta.getId(), pregunta.getOrden(), pregunta.getEnunciado(),
                pregunta.getRespuestas().stream().map(ContenidoParticipacionDTO::opcion).toList());
    }

    private static OpcionVisible opcion(Respuesta respuesta) {
        return new OpcionVisible(respuesta.getId(), respuesta.getCodigo(), respuesta.getTexto());
    }
}
