package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoProgramable;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RubricaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Patrón Adapter (Ports & Adapters): implementa el contrato ConsultaCasosProgramables que usa M3; la llamada pasa por ProxyAcceso de M1 (permiso PROGRAMAR_SESIONES).
@Service
@Transactional(readOnly = true)
public class ServicioIntegracionCasos implements ConsultaCasosProgramables, ConsultaRubricasEvaluacion {

    private final CasoEstudioRepositorio casos;
    private final CasosYaPublicados publicados;

    public ServicioIntegracionCasos(CasoEstudioRepositorio casos, CasosYaPublicados publicados) {
        this.casos = casos;
        this.publicados = publicados;
    }

    @Override
    public List<CasoProgramable> publicados() {
        return casos.listarPorEstado(EstadoCaso.PUBLICADO);
    }

    @Override
    public CasoProgramable consultar(Long idCaso) {
        return CasoProgramable.de(publicados.exigir(idCaso));
    }

    @Override
    public RubricaDTO rubricaDe(Long idCaso) {
        return RubricaDTO.de(publicados.exigir(idCaso).rubricaRegistrada());
    }
}
