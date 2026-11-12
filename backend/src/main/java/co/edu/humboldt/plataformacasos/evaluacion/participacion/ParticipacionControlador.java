package co.edu.humboldt.plataformacasos.evaluacion.participacion;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * RF-12 y RF-15: inicio y fin de cada intento, y consulta de participaciones. Las respuestas y el
 * progreso del intento (RF-10, RF-11) están en el módulo 4: PUT /api/participaciones/{id}/preguntas/{idPregunta}/respuesta.
 */
@RestController
@RequestMapping("/api/participaciones")
public class ParticipacionControlador {

    private final FachadaParticipacion facade;
    private final ConsultaParticipacion consulta;

    public ParticipacionControlador(FachadaParticipacion facade, ConsultaParticipacion consulta) {
        this.facade = facade;
        this.consulta = consulta;
    }

    @GetMapping
    public List<ParticipacionRespuesta> listar(
            @RequestParam(required = false) Long practica,
            @RequestParam(required = false) Long estudiante,
            @RequestParam(required = false) EstadoParticipacion estado,
            @RequestParam(required = false) Integer numeroIntento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return consulta.listar(practica, estudiante, estado, numeroIntento, fechaDesde, fechaHasta);
    }

    @PostMapping("/iniciar")
    public ParticipacionRespuesta iniciar(@Valid @RequestBody IniciarParticipacionRequest request) {
        return facade.iniciar(request);
    }

    @GetMapping("/{id}")
    public ParticipacionRespuesta obtener(@PathVariable Long id) {
        return consulta.obtener(id);
    }

    @PostMapping("/{id}/finalizar")
    public ParticipacionRespuesta finalizar(@PathVariable Long id) {
        return facade.finalizar(id);
    }
}
