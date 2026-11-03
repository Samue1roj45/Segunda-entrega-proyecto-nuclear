package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.Escenario;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosEscenarioRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.EscenarioDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioEscenarios implements GestionEscenarios {

    private final CasoEstudioRepositorio casos;
    private final AccesoCasos acceso;

    public ServicioEscenarios(CasoEstudioRepositorio casos, AccesoCasos acceso) {
        this.casos = casos;
        this.acceso = acceso;
    }

    @Override
    public EscenarioDTO agregar(Long idCaso, DatosEscenarioRequest datos) {
        Escenario escenario = acceso.paraModificar(idCaso).agregarEscenario(datos.nombre(), datos.contexto());
        casos.flush();
        return EscenarioDTO.de(escenario);
    }

    @Override
    @Transactional(readOnly = true)
    public EscenarioDTO consultar(Long idCaso, Long idEscenario) {
        return EscenarioDTO.de(acceso.paraConsultar(idCaso).escenario(idEscenario));
    }

    @Override
    public EscenarioDTO actualizar(Long idCaso, Long idEscenario, DatosEscenarioRequest datos) {
        Escenario escenario = acceso.paraModificar(idCaso).escenario(idEscenario);
        escenario.actualizar(datos.nombre(), datos.contexto());
        return EscenarioDTO.de(escenario);
    }

    @Override
    public void eliminar(Long idCaso, Long idEscenario) {
        CasoEstudio caso = acceso.paraModificar(idCaso);
        caso.eliminarEscenario(idEscenario);
    }
}
