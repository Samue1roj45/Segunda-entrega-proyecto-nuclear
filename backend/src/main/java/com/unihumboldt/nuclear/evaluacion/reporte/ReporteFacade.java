package com.unihumboldt.nuclear.evaluacion.reporte;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.EstudianteRef;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoDenegadoException;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
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
    private final AccesoEvaluacion acceso;

    public ReporteFacade(
            ParticipacionRepository participaciones,
            ResultadoRepository resultados,
            NotaOficialRepository notasOficiales,
            CatalogoAcademico catalogo,
            AccesoEvaluacion acceso) {
        this.participaciones = participaciones;
        this.resultados = resultados;
        this.notasOficiales = notasOficiales;
        this.catalogo = catalogo;
        this.acceso = acceso;
    }

    @Transactional(readOnly = true)
    public ArchivoReporte pdf(Long practicaId, Long estudianteId) {
        byte[] bytes = ReportePdfOpenPdf.generar(filas(practicaId, estudianteId));
        return new ArchivoReporte("reporte-evaluacion.pdf", PDF, bytes);
    }

    @Transactional(readOnly = true)
    public ArchivoReporte excel(Long practicaId, Long estudianteId) {
        byte[] bytes = ReporteExcelPoi.generar(filas(practicaId, estudianteId));
        return new ArchivoReporte("reporte-evaluacion.xlsx", XLSX, bytes);
    }

    List<FilaReporteEvaluacion> filas(Long practicaId, Long estudianteId) {
        ActorActual actor = acceso.exigirActor();
        Long estudianteFiltro = estudianteId;
        if (actor.esEstudiante()) {
            if (estudianteId != null && !Objects.equals(estudianteId, actor.estudianteId())) {
                throw new AccesoDenegadoException("Un estudiante solo puede descargar su propio reporte.");
            }
            estudianteFiltro = actor.estudianteId();
        }
        if (practicaId != null && (actor.esDocente() || actor.esAdmin())) {
            acceso.exigirPracticaDocente(practicaId, actor);
        }
        Map<Long, Resultado> porParticipacion = resultados.findAllByOrderByIdDesc().stream()
                .filter(r -> r.getParticipacion() != null)
                .collect(Collectors.toMap(r -> r.getParticipacion().getId(), Function.identity(), (a, b) -> a));

        Map<String, NotaOficial> oficiales = notasOficiales.findAllByOrderByIdDesc().stream()
                .collect(Collectors.toMap(
                        n -> n.getPracticaId() + ":" + n.getEstudianteId(),
                        Function.identity(),
                        (a, b) -> a));
        Long estudianteFinal = estudianteFiltro;
        return participaciones.findAllByOrderByIdDesc().stream()
                .filter(p -> acceso.visible(actor, p))
                .filter(p -> practicaId == null || Objects.equals(p.getPracticaId(), practicaId))
                .filter(p -> estudianteFinal == null || Objects.equals(p.getEstudianteId(), estudianteFinal))
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
