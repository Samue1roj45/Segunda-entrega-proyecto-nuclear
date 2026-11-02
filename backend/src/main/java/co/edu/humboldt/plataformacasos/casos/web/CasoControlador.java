package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoResumen;
import co.edu.humboldt.plataformacasos.casos.servicio.GestionCasos;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosCasoRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.ResultadoImportacionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** RF-05: registro, consulta y edición del caso, también a partir de un documento DSL-CEPS. */
@RestController
@RequestMapping("/api/casos")
public class CasoControlador {

    private final GestionCasos gestionCasos;

    public CasoControlador(GestionCasos gestionCasos) {
        this.gestionCasos = gestionCasos;
    }

    @GetMapping
    public List<CasoResumen> listar(@RequestParam(required = false) EstadoCaso estado) {
        return gestionCasos.listar(estado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CasoDetalleDTO crear(@RequestBody DatosCasoRequest datos) {
        return gestionCasos.crear(datos);
    }

    @GetMapping("/{idCaso}")
    public CasoDetalleDTO consultar(@PathVariable Long idCaso) {
        return gestionCasos.consultar(idCaso);
    }

    @PutMapping("/{idCaso}")
    public CasoDetalleDTO actualizar(@PathVariable Long idCaso, @RequestBody DatosCasoRequest datos) {
        return gestionCasos.actualizar(idCaso, datos);
    }

    @PostMapping("/importacion-dsl")
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoImportacionDTO importarDsl(@RequestBody DocumentoDsl documento) {
        return gestionCasos.importarDsl(documento);
    }

    @GetMapping("/{idCaso}/dsl")
    public DocumentoDsl exportarDsl(@PathVariable Long idCaso) {
        return gestionCasos.exportarDsl(idCaso);
    }
}
