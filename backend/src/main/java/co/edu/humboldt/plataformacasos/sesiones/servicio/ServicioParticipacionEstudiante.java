package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.sesiones.dto.ContextoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.EstadoIngresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ProgresoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.TiempoRestanteDto;
/*
 #REQUISITO RF-10 || RF-11 || RF-22 || RF-23
 Patrón: PROXY (interfaz común del sujeto real y de su proxy)
 Para qué sirve: contrato de la participación del estudiante. El controlador depende de
 esta interfaz sin saber si habla con el servicio real o con el proxy de control de acceso.
 Iniciar y finalizar el intento son de RF-12 (módulo 5: POST /api/participaciones/iniciar y
 /api/participaciones/{id}/finalizar), que antes de iniciar consulta la regla de ingreso de aquí.
*/
public interface ServicioParticipacionEstudiante {

    /** RF-22: ¿puede el estudiante iniciar ahora? Límite de ingreso y motivo si no. */
    EstadoIngresoDto consultarIngreso(Long idEstudiante, Long idAutorizacion);

    /** RF-10: contextos, preguntas, opciones y selección actual. */
    ContextoDto consultarContexto(Long idEstudiante, Long idParticipacion);

    /** RF-10/11: guarda o cambia la respuesta y devuelve el progreso actualizado. */
    ProgresoDto responder(Long idEstudiante, Long idParticipacion, Long idPregunta, Long idRespuesta);

    /** RF-11: progreso actual. */
    ProgresoDto consultarProgreso(Long idEstudiante, Long idParticipacion);

    /** RF-23: tiempo restante del intento. */
    TiempoRestanteDto consultarTiempo(Long idEstudiante, Long idParticipacion);
}
