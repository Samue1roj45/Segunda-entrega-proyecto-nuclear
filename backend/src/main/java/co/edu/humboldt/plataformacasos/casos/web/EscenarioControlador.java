package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.servicio.GestionEscenarios;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosEscenarioRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.EscenarioDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF-06: escenarios y contextos situacionales del caso. */
@RestController
@RequestMapping("/api/casos/{idCaso}/escenarios")
public class EscenarioControlador {

    private final GestionEscenarios gestionEscenarios;

    public EscenarioControlador(GestionEscenarios gestionEscenarios) {
        this.gestionEscenarios = gestionEscenarios;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EscenarioDTO agregar(@PathVariable Long idCaso, @RequestBody DatosEscenarioRequest datos) {
        return gestionEscenarios.agregar(idCaso, datos);
    }

    @GetMapping("/{idEscenario}")
    public EscenarioDTO consultar(@PathVariable Long idCaso, @PathVariable Long idEscenario) {
        return gestionEscenarios.consultar(idCaso, idEscenario);
    }

    @PutMapping("/{idEscenario}")
    public EscenarioDTO actualizar(@PathVariable Long idCaso, @PathVariable Long idEscenario,
                                   @RequestBody DatosEscenarioRequest datos) {
        return gestionEscenarios.actualizar(idCaso, idEscenario, datos);
    }

    @DeleteMapping("/{idEscenario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idCaso, @PathVariable Long idEscenario) {
        gestionEscenarios.eliminar(idCaso, idEscenario);
    }
}
