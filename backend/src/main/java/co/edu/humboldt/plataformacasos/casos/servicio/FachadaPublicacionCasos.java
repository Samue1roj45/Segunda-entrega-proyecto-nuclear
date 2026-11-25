package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.avisos.ServicioAvisos;
import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.validacion.CasoNoPublicableException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.casos.validacion.InformeValidacion;
import co.edu.humboldt.plataformacasos.casos.validacion.ValidadorDslCeps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Patrón Facade (RF-09): el controlador pide «publicar», «archivar» o «recuperar» sin conocer el orden del
 * proceso. Coordina el acceso al caso, el validador, el cambio de estado y el registro de auditoría; las
 * reglas siguen en cada uno de ellos.
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

    // Opcional: las pruebas unitarias de la fachada la crean sin auditoría.
    @Autowired(required = false)
    private ServicioAvisos avisos;

    @Override
    @Transactional(readOnly = true)
    public InformeValidacion validar(Long idCaso) {
        return validador.validar(acceso.paraConsultar(idCaso));
    }

    @Override
    public CasoDetalleDTO publicar(Long idCaso) {
        CasoEstudio caso = acceso.paraModificar(idCaso);
        caso.exigirPublicable();
        exigirValido(caso);
        caso.publicar();
        registrarCambioDeEstado(caso, null);
        return CasoDetalleDTO.de(caso);
    }

    @Override
    public CasoDetalleDTO archivar(Long idCaso) {
        CasoEstudio caso = acceso.paraModificar(idCaso);
        caso.archivar();
        registrarCambioDeEstado(caso, "CASO_ARCHIVADO");
        return CasoDetalleDTO.de(caso);
    }

    /**
     * Las reglas académicas pudieron cambiar mientras el caso estuvo archivado: se valida otra vez, igual
     * que al publicar. Si ya no las cumple sigue ARCHIVADO y el docente puede duplicarlo para corregirlo.
     */
    @Override
    public CasoDetalleDTO recuperar(Long idCaso) {
        CasoEstudio caso = acceso.paraModificar(idCaso);
        if (!caso.getEstado().puedeRecuperarse()) {
            throw new ConflictoException(
                    "Solo se recupera un caso archivado; este está " + caso.getEstado() + ".");
        }
        exigirValido(caso);
        caso.recuperar();
        registrarCambioDeEstado(caso, "CASO_RECUPERADO");
        return CasoDetalleDTO.de(caso);
    }

    private void exigirValido(CasoEstudio caso) {
        InformeValidacion informe = validador.validar(caso);
        if (!informe.valido()) {
            throw new CasoNoPublicableException(informe);
        }
    }

    /** RNF-08: el cambio de estado queda con fecha en el caso y con su autor en el registro y en la auditoría. */
    private void registrarCambioDeEstado(CasoEstudio caso, String accionAuditada) {
        LOG.info("Caso {} pasó a {} por el docente {}", caso.getId(), caso.getEstado(), caso.getDocenteId());
        if (avisos == null || accionAuditada == null) { return; }
        var actor = acceso.actor();
        String nombre = caso.getNombre() == null || caso.getNombre().isBlank() ? "(sin nombre)" : caso.getNombre();
        String verbo = "CASO_ARCHIVADO".equals(accionAuditada) ? " archivó el caso «" : " recuperó el caso archivado «";
        avisos.registrar(accionAuditada, actor.id(), actor.correoInstitucional(), actor.nombreCompleto() + verbo + nombre + "»");
    }
}
