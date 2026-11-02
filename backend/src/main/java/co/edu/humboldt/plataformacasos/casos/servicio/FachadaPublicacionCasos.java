package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.casos.validacion.InformeValidacion;
import co.edu.humboldt.plataformacasos.casos.validacion.ValidadorDslCeps;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Patrón Facade (RF-09): coordina el acceso al caso y su validador sin duplicar sus reglas. */
@Service
@Transactional
public class FachadaPublicacionCasos implements PublicacionCasos {

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
}
