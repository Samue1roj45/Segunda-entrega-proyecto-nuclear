package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.servicio.PublicacionCasos;
import co.edu.humboldt.plataformacasos.casos.validacion.InformeValidacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF-09: validación académica del caso. */
@RestController
@RequestMapping("/api/casos/{idCaso}")
public class PublicacionControlador {

    private final PublicacionCasos publicacion;

    public PublicacionControlador(PublicacionCasos publicacion) {
        this.publicacion = publicacion;
    }

    @GetMapping("/validacion")
    public InformeValidacion validar(@PathVariable Long idCaso) {
        return publicacion.validar(idCaso);
    }
}
