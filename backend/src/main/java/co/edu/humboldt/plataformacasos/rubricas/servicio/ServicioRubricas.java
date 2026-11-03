package co.edu.humboldt.plataformacasos.rubricas.servicio;

import co.edu.humboldt.plataformacasos.casos.dominio.CriterioEvaluacion;
import co.edu.humboldt.plataformacasos.casos.dominio.Rubrica;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.casos.servicio.AccesoCasos;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CriterioDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosCriterioRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosRubricaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RubricaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioRubricas implements GestionRubricas {

    private final CasoEstudioRepositorio casos;
    private final AccesoCasos acceso;

    public ServicioRubricas(CasoEstudioRepositorio casos, AccesoCasos acceso) {
        this.casos = casos;
        this.acceso = acceso;
    }

    @Override
    public RubricaDTO registrar(Long idCaso, DatosRubricaRequest datos) {
        Rubrica rubrica = acceso.paraModificar(idCaso).registrarRubrica(datos.nombre(), datos.descripcion());
        casos.flush();
        return RubricaDTO.de(rubrica);
    }

    @Override
    @Transactional(readOnly = true)
    public RubricaDTO consultar(Long idCaso) {
        return RubricaDTO.de(acceso.paraConsultar(idCaso).rubricaRegistrada());
    }

    @Override
    public void retirar(Long idCaso) {
        acceso.paraModificar(idCaso).retirarRubrica();
    }

    @Override
    public CriterioDTO agregarCriterio(Long idCaso, DatosCriterioRequest datos) {
        CriterioEvaluacion criterio = acceso.paraModificar(idCaso).rubricaRegistrada()
                .agregarCriterio(datos.criterio(), datos.parametros(), datos.min(), datos.max());
        casos.flush();
        return CriterioDTO.de(criterio);
    }

    @Override
    public CriterioDTO actualizarCriterio(Long idCaso, Long idCriterio, DatosCriterioRequest datos) {
        CriterioEvaluacion criterio = acceso.paraModificar(idCaso).rubricaRegistrada().criterio(idCriterio);
        criterio.actualizar(datos.criterio(), datos.parametros(), datos.min(), datos.max());
        return CriterioDTO.de(criterio);
    }

    @Override
    public void eliminarCriterio(Long idCaso, Long idCriterio) {
        acceso.paraModificar(idCaso).rubricaRegistrada().eliminarCriterio(idCriterio);
    }
}
