package com.unihumboldt.nuclear.evaluacion.resultado;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.unihumboldt.nuclear.evaluacion.notificacion.ResumenEnvio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/resultados")
public class ResultadoController {

    private final EvaluacionFacade facade;
    private final ConsultaResultado consulta;

    public ResultadoController(EvaluacionFacade facade, ConsultaResultado consulta) {
        this.facade = facade;
        this.consulta = consulta;
    }

    @GetMapping
    public List<ResultadoRespuesta> listar(
            @RequestParam(required = false) Long practica,
            @RequestParam(required = false) Long estudiante) {
        return consulta.listar(practica, estudiante);
    }

    @GetMapping("/notas-oficiales")
    public List<NotaOficialRespuesta> listarOficiales(
            @RequestParam(required = false) Long practica,
            @RequestParam(required = false) Long estudiante) {
        return consulta.listarOficiales(practica, estudiante);
    }

    @PostMapping("/nota-oficial")
    public NotaOficialRespuesta seleccionarOficial(@Valid @RequestBody SeleccionarNotaOficialRequest request) {
        return facade.seleccionarOficial(request);
    }

    @GetMapping("/{id}")
    public ResultadoRespuesta obtener(@PathVariable Long id) {
        return consulta.obtener(id);
    }

    @PostMapping("/practica/{practicaId}/evaluar-pendientes")
    public EvaluacionPendientesRespuesta evaluarPendientes(@PathVariable Long practicaId) {
        return facade.evaluarPendientes(practicaId);
    }

    @PostMapping("/practica/{practicaId}/publicar-calificaciones")
    public ResumenEnvio publicarCalificaciones(@PathVariable Long practicaId) {
        return facade.publicarCalificaciones(practicaId);
    }
}
