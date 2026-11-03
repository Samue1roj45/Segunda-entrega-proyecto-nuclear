package com.unihumboldt.nuclear.evaluacion.notificacion;

import java.util.List;

/**
 * Sujeto del Proxy de retroalimentación docente (RF-26).
 */
public interface GestionRetroalimentacion {

    RetroalimentacionRespuesta registrar(RegistrarRetroalimentacionRequest request);

    RetroalimentacionRespuesta obtener(Long id);

    List<RetroalimentacionRespuesta> listar(Long practicaId, Long participacionId);
}
