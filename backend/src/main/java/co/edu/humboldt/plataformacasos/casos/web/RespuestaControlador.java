package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.servicio.GestionRespuestas;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosRespuestaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.MarcarCorrectaRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.PreguntaDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RespuestaDTO;
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

/** RF-07: opciones de respuesta de cada pregunta. */
@RestController
@RequestMapping("/api/casos/{idCaso}")
public class RespuestaControlador {

    private final GestionRespuestas gestionRespuestas;

    public RespuestaControlador(GestionRespuestas gestionRespuestas) {
        this.gestionRespuestas = gestionRespuestas;
    }

    @PostMapping("/preguntas/{idPregunta}/respuestas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaDTO agregar(@PathVariable Long idCaso, @PathVariable Long idPregunta,
                                @RequestBody DatosRespuestaRequest datos) {
        return gestionRespuestas.agregar(idCaso, idPregunta, datos);
    }

    @PutMapping("/preguntas/{idPregunta}/respuesta-correcta")
    public PreguntaDTO marcarCorrecta(@PathVariable Long idCaso, @PathVariable Long idPregunta,
                                      @RequestBody MarcarCorrectaRequest solicitud) {
        return gestionRespuestas.marcarCorrecta(idCaso, idPregunta, solicitud);
    }

    @GetMapping("/respuestas/{idRespuesta}")
    public RespuestaDTO consultar(@PathVariable Long idCaso, @PathVariable Long idRespuesta) {
        return gestionRespuestas.consultar(idCaso, idRespuesta);
    }

    @PutMapping("/respuestas/{idRespuesta}")
    public RespuestaDTO actualizar(@PathVariable Long idCaso, @PathVariable Long idRespuesta,
                                   @RequestBody DatosRespuestaRequest datos) {
        return gestionRespuestas.actualizar(idCaso, idRespuesta, datos);
    }

    @DeleteMapping("/respuestas/{idRespuesta}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idCaso, @PathVariable Long idRespuesta) {
        gestionRespuestas.eliminar(idCaso, idRespuesta);
    }
}
