package co.edu.humboldt.plataformacasos.sesiones.repositorio;

import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.IntentoParticipacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RegistroParticipaciones;
import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosRepeticion;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
 #REQUISITO RF-25
 Para qué sirve: acceso a `autorizaciones_repeticion` (cada fila concede UN intento extra) y armado de
 los datos que necesita la cadena de validación: estado de la autorización y de la sesión, docente
 creador del caso (módulo 2) e intentos usados (módulo 5; una ausencia no cuenta).
*/
@Repository
public class RepeticionRepositorio {

    private final JdbcTemplate jdbc;
    private final AutorizacionRepositorio autorizaciones;
    private final SesionRepositorio sesiones;
    private final ContenidoCasoInterno casos;
    private final RegistroParticipaciones participaciones;
    private final Clock reloj;

    public RepeticionRepositorio(JdbcTemplate jdbc, AutorizacionRepositorio autorizaciones,
                                 SesionRepositorio sesiones, ContenidoCasoInterno casos,
                                 RegistroParticipaciones participaciones, Clock reloj) {
        this.jdbc = jdbc;
        this.autorizaciones = autorizaciones;
        this.sesiones = sesiones;
        this.casos = casos;
        this.participaciones = participaciones;
        this.reloj = reloj;
    }

    public Optional<DatosRepeticion> cargar(Long idAutorizacion) {
        return autorizaciones.buscar(idAutorizacion).flatMap(a -> sesiones.buscar(a.idPractica()).map(p -> {
            int usados = (int) participaciones.intentosDeAutorizacion(idAutorizacion).stream()
                    .filter(IntentoParticipacion::cuentaComoIntento).count();
            Long creador = casos.resumen(p.idCaso()).docenteId();
            return new DatosRepeticion(a.estado(), creador, p.idDocente(), p.estado(), usados,
                    contarRepeticiones(idAutorizacion));
        }));
    }

    public int contarRepeticiones(Long idAutorizacion) {
        return autorizaciones.contarRepeticiones(idAutorizacion);
    }

    public void autorizar(Long idAutorizacion, Long idDocente, String motivo) {
        jdbc.update("INSERT INTO autorizaciones_repeticion (id_autorizacion, id_docente, motivo, fecha_autorizacion) "
                + "VALUES (?, ?, ?, ?)", idAutorizacion, idDocente, motivo, Timestamp.valueOf(LocalDateTime.now(reloj)));
    }
}
