package co.edu.humboldt.plataformacasos.casos.web;

import co.edu.humboldt.plataformacasos.casos.integracion.ConsultaCasosProgramables;
import co.edu.humboldt.plataformacasos.casos.integracion.ConsultaRubricasEvaluacion;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoProgramable;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.RubricaDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Contratos de consulta para los Módulos 3 y 4. Ninguno revela la clave de las preguntas. */
@RestController
@RequestMapping("/api/integracion/casos")
public class IntegracionCasosControlador {

    private final ConsultaCasosProgramables casosProgramables;
    private final ConsultaRubricasEvaluacion rubricasEvaluacion;

    public IntegracionCasosControlador(ConsultaCasosProgramables casosProgramables,
                                       ConsultaRubricasEvaluacion rubricasEvaluacion) {
        this.casosProgramables = casosProgramables;
        this.rubricasEvaluacion = rubricasEvaluacion;
    }

    @GetMapping("/publicados")
    public List<CasoProgramable> publicados() {
        return casosProgramables.publicados();
    }

    @GetMapping("/{idCaso}/programacion")
    public CasoProgramable programacion(@PathVariable Long idCaso) {
        return casosProgramables.consultar(idCaso);
    }

    @GetMapping("/{idCaso}/rubrica")
    public RubricaDTO rubrica(@PathVariable Long idCaso) {
        return rubricasEvaluacion.rubricaDe(idCaso);
    }
}
