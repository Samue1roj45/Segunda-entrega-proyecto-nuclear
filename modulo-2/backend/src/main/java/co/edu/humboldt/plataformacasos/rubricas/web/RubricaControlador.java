package co.edu.humboldt.plataformacasos.rubricas.web;

import co.edu.humboldt.plataformacasos.casos.servicio.dto.CriterioDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosCriterioRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosRubricaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RubricaDTO;
import co.edu.humboldt.plataformacasos.rubricas.servicio.GestionRubricas;
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

/** RF-17: rúbrica del caso y sus criterios de evaluación. */
@RestController
@RequestMapping("/api/casos/{idCaso}/rubrica")
public class RubricaControlador {

    private final GestionRubricas gestionRubricas;

    public RubricaControlador(GestionRubricas gestionRubricas) {
        this.gestionRubricas = gestionRubricas;
    }

    @PutMapping
    public RubricaDTO registrar(@PathVariable Long idCaso, @RequestBody DatosRubricaRequest datos) {
        return gestionRubricas.registrar(idCaso, datos);
    }

    @GetMapping
    public RubricaDTO consultar(@PathVariable Long idCaso) {
        return gestionRubricas.consultar(idCaso);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirar(@PathVariable Long idCaso) {
        gestionRubricas.retirar(idCaso);
    }

    @PostMapping("/criterios")
    @ResponseStatus(HttpStatus.CREATED)
    public CriterioDTO agregarCriterio(@PathVariable Long idCaso, @RequestBody DatosCriterioRequest datos) {
        return gestionRubricas.agregarCriterio(idCaso, datos);
    }

    @PutMapping("/criterios/{idCriterio}")
    public CriterioDTO actualizarCriterio(@PathVariable Long idCaso, @PathVariable Long idCriterio,
                                          @RequestBody DatosCriterioRequest datos) {
        return gestionRubricas.actualizarCriterio(idCaso, idCriterio, datos);
    }

    @DeleteMapping("/criterios/{idCriterio}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCriterio(@PathVariable Long idCaso, @PathVariable Long idCriterio) {
        gestionRubricas.eliminarCriterio(idCaso, idCriterio);
    }
}
