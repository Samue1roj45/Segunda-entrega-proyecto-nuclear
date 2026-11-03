package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.servicio.GestionPreguntas;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosPreguntaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.PreguntaDTO;
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

/** RF-06 y RF-07: preguntas de cada escenario. */
@RestController
@RequestMapping("/api/casos/{idCaso}")
public class PreguntaControlador {

    private final GestionPreguntas gestionPreguntas;

    public PreguntaControlador(GestionPreguntas gestionPreguntas) {
        this.gestionPreguntas = gestionPreguntas;
    }

    @PostMapping("/escenarios/{idEscenario}/preguntas")
    @ResponseStatus(HttpStatus.CREATED)
    public PreguntaDTO agregar(@PathVariable Long idCaso, @PathVariable Long idEscenario,
                               @RequestBody DatosPreguntaRequest datos) {
        return gestionPreguntas.agregar(idCaso, idEscenario, datos);
    }

    @GetMapping("/preguntas/{idPregunta}")
    public PreguntaDTO consultar(@PathVariable Long idCaso, @PathVariable Long idPregunta) {
        return gestionPreguntas.consultar(idCaso, idPregunta);
    }

    @PutMapping("/preguntas/{idPregunta}")
    public PreguntaDTO actualizar(@PathVariable Long idCaso, @PathVariable Long idPregunta,
                                  @RequestBody DatosPreguntaRequest datos) {
        return gestionPreguntas.actualizar(idCaso, idPregunta, datos);
    }

    @DeleteMapping("/preguntas/{idPregunta}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idCaso, @PathVariable Long idPregunta) {
        gestionPreguntas.eliminar(idCaso, idPregunta);
    }
}
