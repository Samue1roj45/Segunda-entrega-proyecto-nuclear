package com.unihumboldt.nuclear.evaluacion.reporte;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoDenegadoException;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;

/**
 * Protection Proxy RF-16: el estudiante no descarga ajenos; el docente no descarga otra práctica.
 * No recalcula ni cambia notas de intento.
 */
@Service
public class ReporteDescargaProxy implements DescargaReporte {

    private final ReporteFacade real;
    private final AccesoEvaluacion acceso;

    public ReporteDescargaProxy(ReporteFacade real, AccesoEvaluacion acceso) {
        this.real = real;
        this.acceso = acceso;
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoReporte pdf(Long practicaId, Long estudianteId) {
        return real.pdfDe(filasAutorizadas(practicaId, estudianteId));
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoReporte excel(Long practicaId, Long estudianteId) {
        return real.excelDe(filasAutorizadas(practicaId, estudianteId));
    }

    private List<FilaReporteEvaluacion> filasAutorizadas(Long practicaId, Long estudianteId) {
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
        return real.filas(practicaId, estudianteFiltro).stream()
                .filter(fila -> acceso.visible(actor, fila.practicaId(), fila.estudianteId()))
                .toList();
    }
}
