package com.unihumboldt.nuclear.evaluacion.resultado;

import java.util.List;

/**
 * Sujeto del Proxy de consulta de resultados y nota oficial (RF-14).
 */
public interface ConsultaResultado {

    ResultadoRespuesta obtener(Long id);

    List<ResultadoRespuesta> listar(Long practicaId, Long estudianteId);

    List<NotaOficialRespuesta> listarOficiales(Long practicaId, Long estudianteId);
}
