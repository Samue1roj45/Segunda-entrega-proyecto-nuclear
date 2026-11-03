package co.edu.humboldt.plataformacasos.casos.servicio.dto;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;

import java.time.LocalDateTime;
import java.util.List;

/** Vista completa del caso para su docente. Nunca debe entregarse a un estudiante. */
public record CasoDetalleDTO(Long id, Long docenteId, String nombre, String desarrolloSituacional,
                             String contextoHistoria, Integer tiempoEstimadoMinutos, EstadoCaso estado,
                             LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion,
                             LocalDateTime fechaPublicacion, LocalDateTime fechaArchivado,
                             List<EscenarioDTO> escenarios, RubricaDTO rubrica) {

    public static CasoDetalleDTO de(CasoEstudio caso) {
        return new CasoDetalleDTO(caso.getId(), caso.getDocenteId(), caso.getNombre(),
                caso.getDesarrolloSituacional(), caso.getContextoHistoria(), caso.getTiempoEstimadoMinutos(),
                caso.getEstado(), caso.getFechaCreacion(), caso.getFechaActualizacion(),
                caso.getFechaPublicacion(), caso.getFechaArchivado(),
                caso.getEscenarios().stream().map(EscenarioDTO::de).toList(),
                caso.getRubrica() == null ? null : RubricaDTO.de(caso.getRubrica()));
    }
}
