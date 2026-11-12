package com.example.demo.service;

import com.example.demo.dto.ContextoDto;
import com.example.demo.dto.EstadoIngresoDto;
import com.example.demo.dto.FinalizacionDto;
import com.example.demo.dto.ProgresoDto;
import com.example.demo.dto.TiempoRestanteDto;
/*
 #REQUISITO RF-10 || RF-11 || RF-22 || RF-23
 Patrón: PROXY (interfaz común del sujeto real y de su proxy)
 Para qué sirve: contrato de la participación del estudiante. El controlador depende de
 esta interfaz sin saber si habla con el servicio real o con el proxy de control de acceso.
*/
public interface ParticipacionService {

    /** RF-22: ¿puede el estudiante iniciar ahora? Límite de ingreso y motivo si no. */
    EstadoIngresoDto consultarIngreso(Long idEstudiante, Long idAutorizacion);

    /** Inicia (o retoma) la participación de la autorización. Devuelve id_participacion. */
    Long iniciar(Long idEstudiante, Long idAutorizacion);

    /** RF-10: contextos, preguntas, opciones y selección actual. */
    ContextoDto consultarContexto(Long idEstudiante, Long idParticipacion);

    /** RF-10/11: guarda o cambia la respuesta y devuelve el progreso actualizado. */
    ProgresoDto responder(Long idEstudiante, Long idParticipacion, Long idPregunta, Long idRespuesta);

    /** RF-11: progreso actual. */
    ProgresoDto consultarProgreso(Long idEstudiante, Long idParticipacion);

    /** RF-23: tiempo restante del intento. */
    TiempoRestanteDto consultarTiempo(Long idEstudiante, Long idParticipacion);

    /** RF-23/25: el estudiante termina su intento (se califica). */
    FinalizacionDto finalizar(Long idEstudiante, Long idParticipacion);
}
