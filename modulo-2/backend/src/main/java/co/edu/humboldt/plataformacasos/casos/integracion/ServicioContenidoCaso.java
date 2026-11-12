package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoProgramable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Patrón Adapter (Ports & Adapters): implementa ContenidoCasoInterno para que M4 y M5 lean preguntas y opciones del caso.
@Service
@Transactional(readOnly = true)
public class ServicioContenidoCaso implements ContenidoCasoInterno {

    private final CasosYaPublicados publicados;
    private final CasoEstudioRepositorio casos;

    public ServicioContenidoCaso(CasosYaPublicados publicados, CasoEstudioRepositorio casos) {
        this.publicados = publicados;
        this.casos = casos;
    }

    @Override
    public ContenidoParticipacionDTO contenidoSinClaves(Long idCaso) {
        return ContenidoParticipacionDTO.de(publicados.exigir(idCaso));
    }

    @Override
    public ClaveEvaluacionDTO claveDeEvaluacion(Long idCaso) {
        return ClaveEvaluacionDTO.de(publicados.exigir(idCaso));
    }

    @Override
    public CasoProgramable resumen(Long idCaso) {
        return CasoProgramable.de(publicados.exigir(idCaso));
    }

    @Override
    public Optional<Long> casoDeRespuesta(Long idRespuesta) {
        return casos.casoDeRespuesta(idRespuesta);
    }
}
