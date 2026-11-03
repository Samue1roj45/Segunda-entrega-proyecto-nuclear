package com.unihumboldt.nuclear.evaluacion.reporte;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.EstudianteRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.participacion.ParticipacionRepository;
import com.unihumboldt.nuclear.evaluacion.resultado.NotaOficial;
import com.unihumboldt.nuclear.evaluacion.resultado.NotaOficialRepository;
import com.unihumboldt.nuclear.evaluacion.resultado.Resultado;
import com.unihumboldt.nuclear.evaluacion.resultado.ResultadoRepository;

@Service
public class ReporteFacade {

    public static final String PDF = "application/pdf";
    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ParticipacionRepository participaciones;
    private final ResultadoRepository resultados;
    private final NotaOficialRepository notasOficiales;
    private final CatalogoAcademico catalogo;

    public ReporteFacade(
            ParticipacionRepository participaciones,
            ResultadoRepository resultados,
            NotaOficialRepository notasOficiales,
            CatalogoAcademico catalogo) {
        this.participaciones = participaciones;
        this.resultados = resultados;
        this.notasOficiales = notasOficiales;
        this.catalogo = catalogo;
    }

    ArchivoReporte pdfDe(List<FilaReporteEvaluacion> filas) {
        return new ArchivoReporte("reporte-evaluacion.pdf", PDF, ReportePdfOpenPdf.generar(filas));
    }

    ArchivoReporte excelDe(List<FilaReporteEvaluacion> filas) {
        return new ArchivoReporte("reporte-evaluacion.xlsx", XLSX, ReporteExcelPoi.generar(filas));
    }

    List<FilaReporteEvaluacion> filas(Long practicaId, Long estudianteId) {
        Map<Long, Resultado> porParticipacion = resultados.findAllByOrderByIdDesc().stream()
                .filter(r -> r.getParticipacion() != null)
                .collect(Collectors.toMap(r -> r.getParticipacion().getId(), Function.identity(), (a, b) -> a));

        Map<String, NotaOficial> oficiales = notasOficiales.findAllByOrderByIdDesc().stream()
                .collect(Collectors.toMap(
                        n -> n.getPracticaId() + ":" + n.getEstudianteId(),
                        Function.identity(),
                        (a, b) -> a));
        return participaciones.findAllByOrderByIdDesc().stream()
                .filter(p -> practicaId == null || Objects.equals(p.getPracticaId(), practicaId))
                .filter(p -> estudianteId == null || Objects.equals(p.getEstudianteId(), estudianteId))
                .sorted(Comparator
                        .comparing(Participacion::getPracticaId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(Participacion::getEstudianteId, Comparator.nullsLast(Long::compareTo))
                        .thenComparingInt(Participacion::getNumeroIntento))
                .map(p -> toFila(p, porParticipacion.get(p.getId()), oficiales, porParticipacion))
                .toList();
    }

    private FilaReporteEvaluacion toFila(
            Participacion participacion,
            Resultado resultado,
            Map<String, NotaOficial> oficiales,
            Map<Long, Resultado> porParticipacion) {
        PracticaRef practica = catalogo.buscarPractica(participacion.getPracticaId()).orElse(null);
        EstudianteRef estudiante = catalogo.buscarEstudiante(participacion.getEstudianteId()).orElse(null);
        NotaOficial seleccion = oficiales.get(participacion.getPracticaId() + ":" + participacion.getEstudianteId());
        boolean oficial = seleccion != null
                && Objects.equals(seleccion.getParticipacion().getId(), participacion.getId());
        java.math.BigDecimal notaOficial = null;
        if (seleccion != null) {
            Resultado oficialRes = porParticipacion.get(seleccion.getParticipacion().getId());
            notaOficial = oficialRes == null ? null : oficialRes.getNotaFinal();
        }
        return new FilaReporteEvaluacion(
                participacion.getId(),
                resultado == null ? null : resultado.getId(),
                participacion.getPracticaId(),
                practica == null ? null : practica.nombre(),
                participacion.getEstudianteId(),
                estudiante == null ? null : estudiante.correo(),
                estudiante == null ? null : estudiante.nombreCompleto(),
                participacion.getNumeroIntento(),
                participacion.getEstado(),
                ClasificacionReporte.de(participacion, resultado),
                resultado == null ? null : resultado.getCorrectas(),
                resultado == null ? null : resultado.getIncorrectas(),
                resultado == null ? null : resultado.getNoRespondidas(),
                resultado == null ? null : resultado.getPesoObtenido(),
                resultado == null ? null : resultado.getPesoTotal(),
                resultado == null ? null : resultado.getEscalaMaxima(),
                resultado == null ? null : resultado.getNotaFinal(),
                oficial,
                notaOficial,
                participacion.getInicio(),
                participacion.getFin());
    }
}
