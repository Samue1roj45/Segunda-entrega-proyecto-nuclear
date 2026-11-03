package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import co.edu.humboldt.plataformacasos.casos.dominio.Escenario;

import java.util.List;

public record EscenarioDTO(Long id, String nombre, String contexto, int orden, List<PreguntaDTO> preguntas) {

    public static EscenarioDTO de(Escenario escenario) {
        return new EscenarioDTO(escenario.getId(), escenario.getNombre(), escenario.getContexto(),
                escenario.getOrden(), escenario.getPreguntas().stream().map(PreguntaDTO::de).toList());
    }
}
