package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.validacion.CasoNoPublicableException;
import co.edu.humboldt.plataformacasos.casos.validacion.InformeValidacion;
import co.edu.humboldt.plataformacasos.casos.validacion.ValidadorDslCeps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Patrón Facade (RF-09): el controlador pide «publicar» sin conocer el orden del proceso. Coordina el
 * acceso al caso, el validador y el cambio de estado; las reglas siguen en cada uno de ellos.
 */
@Service
@Transactional
public class FachadaPublicacionCasos implements PublicacionCasos {

    private static final Logger LOG = LoggerFactory.getLogger(FachadaPublicacionCasos.class);

    private final AccesoCasos acceso;
    private final ValidadorDslCeps validador;

    public FachadaPublicacionCasos(AccesoCasos acceso, ValidadorDslCeps validador) {
        this.acceso = acceso;
        this.validador = validador;
    }

    @Override
    @Transactional(readOnly = true)
    public InformeValidacion validar(Long idCaso) {
        return validador.validar(acceso.paraConsultar(idCaso));
    }

    @Override
    public CasoDetalleDTO publicar(Long idCaso) {
        CasoEstudio caso = acceso.paraModificar(idCaso);
        caso.exigirTransicionA(EstadoCaso.PUBLICADO);
        InformeValidacion informe = validador.validar(caso);
        if (!informe.valido()) {
            throw new CasoNoPublicableException(informe);
        }
        caso.publicar();
        registrarCambioDeEstado(caso);
        return CasoDetalleDTO.de(caso);
    }

    @Override
    public CasoDetalleDTO archivar(Long idCaso) {
        CasoEstudio caso = acceso.paraModificar(idCaso);
        caso.archivar();
        registrarCambioDeEstado(caso);
        return CasoDetalleDTO.de(caso);
    }

    /** RNF-08: el cambio de estado queda con fecha en el caso y con su autor en el registro. */
    private void registrarCambioDeEstado(CasoEstudio caso) {
        LOG.info("Caso {} pasó a {} por el docente {}", caso.getId(), caso.getEstado(), caso.getDocenteId());
    }
}
