package com.unihumboldt.nuclear.evaluacion.reporte;

/**
 * Sujeto del Proxy de descarga de reportes (RF-16).
 */
public interface DescargaReporte {

    ArchivoReporte pdf(Long practicaId, Long estudianteId);

    ArchivoReporte excel(Long practicaId, Long estudianteId);
}
