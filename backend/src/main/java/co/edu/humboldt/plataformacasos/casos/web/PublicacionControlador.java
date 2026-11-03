package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.servicio.PublicacionCasos;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.validacion.InformeValidacion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** RF-09: validación académica, publicación y archivo del caso. */
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

    @PostMapping("/publicacion")
    public CasoDetalleDTO publicar(@PathVariable Long idCaso) {
        return publicacion.publicar(idCaso);
    }

    @PostMapping("/archivo")
    public CasoDetalleDTO archivar(@PathVariable Long idCaso) {
        return publicacion.archivar(idCaso);
    }
}
