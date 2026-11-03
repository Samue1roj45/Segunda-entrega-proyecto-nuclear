package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosEscenarioRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.EscenarioDTO;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

/** RF-06: escenarios académicos de un caso. */
public interface GestionEscenarios {

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    EscenarioDTO agregar(Long idCaso, DatosEscenarioRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    EscenarioDTO consultar(Long idCaso, Long idEscenario);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    EscenarioDTO actualizar(Long idCaso, Long idEscenario, DatosEscenarioRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    void eliminar(Long idCaso, Long idEscenario);
}
