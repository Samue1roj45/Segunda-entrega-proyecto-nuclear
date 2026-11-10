package co.edu.humboldt.plataformacasos.casos.integracion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioContenidoCaso implements ContenidoCasoInterno {

    private final CasosYaPublicados publicados;

    public ServicioContenidoCaso(CasosYaPublicados publicados) {
        this.publicados = publicados;
    }

    @Override
    public ContenidoParticipacionDTO contenidoSinClaves(Long idCaso) {
        return ContenidoParticipacionDTO.de(publicados.exigir(idCaso));
    }

    @Override
    public ClaveEvaluacionDTO claveDeEvaluacion(Long idCaso) {
        return ClaveEvaluacionDTO.de(publicados.exigir(idCaso));
    }
}
