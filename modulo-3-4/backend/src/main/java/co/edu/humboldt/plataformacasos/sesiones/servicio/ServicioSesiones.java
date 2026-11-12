package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.casos.integracion.ConsultaCasosProgramables;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoProgramable;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.IntentoParticipacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.RegistroParticipaciones;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Autorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.DatosRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SesionProgramada;
import co.edu.humboldt.plataformacasos.sesiones.dto.AutorizacionEstudianteDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.ConvocadoDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionCreadaResponse;
import co.edu.humboldt.plataformacasos.sesiones.evento.EstudianteConvocadoEvento;
import co.edu.humboldt.plataformacasos.sesiones.evento.SesionModificadaEvento;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.AutorizacionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.RepeticionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.SesionRepositorio;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-04 || RF-18 || RF-20
 Para qué sirve: casos de uso de programación. Agendar + asociar estudiantes ocurre en UNA
 transacción: si un estudiante es inválido, no queda la sesión a medias. Cada estudiante convocado
 recibe un código de acceso nuevo (solo se guarda su hash) y se publica EstudianteConvocadoEvento
 para que el módulo 6 le envíe la convocatoria por correo (RF-21).
 Usa el módulo 2 para validar el caso (ConsultaCasosProgramables, protegido con PROGRAMAR_SESIONES)
 y el módulo 1 para validar que cada id sea un estudiante activo.
 */
@Service
public class ServicioSesiones {

    private final SesionRepositorio sesiones;
    private final AutorizacionRepositorio autorizaciones;
    private final RepeticionRepositorio repeticiones;
    private final ConsultaCasosProgramables casosProgramables;
    private final ContenidoCasoInterno casos;
    private final UsuarioRepositorio usuarios;
    private final RegistroParticipaciones participaciones;
    private final GeneradorCodigoAcceso generador;
    private final PasswordEncoder codificador;
    private final ApplicationEventPublisher eventos;
    private final Clock reloj;

    public ServicioSesiones(SesionRepositorio sesiones, AutorizacionRepositorio autorizaciones,
                            RepeticionRepositorio repeticiones, ConsultaCasosProgramables casosProgramables,
                            ContenidoCasoInterno casos, UsuarioRepositorio usuarios,
                            RegistroParticipaciones participaciones, GeneradorCodigoAcceso generador,
                            PasswordEncoder codificador, ApplicationEventPublisher eventos, Clock reloj) {
        this.sesiones = sesiones;
        this.autorizaciones = autorizaciones;
        this.repeticiones = repeticiones;
        this.casosProgramables = casosProgramables;
        this.casos = casos;
        this.usuarios = usuarios;
        this.participaciones = participaciones;
        this.generador = generador;
        this.codificador = codificador;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    // ---------- RF-04 + RF-18: agendar ----------

    @Transactional
    public SesionCreadaResponse agendarConEstudiantes(SesionProgramada s) {
        validarCaso(s.idCaso(), s.tiempoMaximoMinutos());
        Long idPractica = sesiones.agendar(s);
        for (Long idEstudiante : s.idsEstudiantes()) {
            convocar(idPractica, idEstudiante);                                   // RF-04
        }
        return new SesionCreadaResponse(idPractica, listarConvocados(idPractica, s.idDocente()));
    }

    /** RF-18: cambia las condiciones de una sesión no finalizada y avisa a los convocados (RF-21). */
    @Transactional
    public void modificarCondiciones(Long idPractica, SesionProgramada s) {
        Practica actual = sesionDelDocente(idPractica, s.idDocente());
        if (actual.finalizada()) {
            throw new ReglaNegocioException("La sesión ya finalizó; no se pueden cambiar sus condiciones");
        }
        validarTiempoDelCaso(casos.resumen(actual.idCaso()), s.tiempoMaximoMinutos());
        sesiones.actualizarCondiciones(idPractica, s);
        eventos.publishEvent(new SesionModificadaEvento(idPractica, "CONDICIONES", LocalDateTime.now(reloj)));
    }

    // ---------- RF-04: estudiantes de la sesión ----------

    @Transactional
    public Long agregarEstudiante(Long idPractica, Long idDocente, Long idEstudiante) {
        Practica practica = sesionDelDocente(idPractica, idDocente);
        if (practica.finalizada()) {
            throw new ReglaNegocioException("La sesión ya finalizó; no se pueden agregar estudiantes");
        }
        return convocar(idPractica, idEstudiante);
    }

    @Transactional
    public void retirarEstudiante(Long idPractica, Long idDocente, Long idEstudiante) {
        sesionDelDocente(idPractica, idDocente);
        if (!autorizaciones.retirar(idPractica, idEstudiante)) {
            throw new RecursoNoEncontradoException("El estudiante no está convocado a esta sesión");
        }
    }

    @Transactional(readOnly = true)
    public List<ConvocadoDto> listarConvocados(Long idPractica, Long idDocente) {
        sesionDelDocente(idPractica, idDocente);
        return autorizaciones.dePractica(idPractica).stream().map(this::convocado).toList();
    }

    // ---------- RF-20: lo que ve el estudiante después de iniciar sesión ----------

    @Transactional(readOnly = true)
    public List<AutorizacionEstudianteDto> autorizacionesDeEstudiante(Long idEstudiante) {
        return autorizaciones.activasDeEstudiante(idEstudiante).stream()
                .flatMap(a -> sesiones.buscar(a.idPractica()).stream().map(p -> paraEstudiante(a, p)))
                .toList();
    }

    // ---------- apoyo ----------

    /** Crea (o reactiva) la autorización con un código nuevo y avisa al módulo de notificaciones. */
    private Long convocar(Long idPractica, Long idEstudiante) {
        exigirEstudianteActivo(idEstudiante);
        String codigo = generador.generar();
        String hash = codificador.encode(codigo);
        Optional<Autorizacion> existente = autorizaciones.buscar(idPractica, idEstudiante);
        Long idAutorizacion;
        if (existente.isPresent()) {
            if (existente.get().activa()) {
                throw new ConflictoException("El estudiante " + idEstudiante + " ya está convocado a esta sesión");
            }
            idAutorizacion = existente.get().id();
            autorizaciones.reactivar(idAutorizacion, hash);
        } else {
            idAutorizacion = autorizaciones.insertar(idPractica, idEstudiante, hash);
        }
        eventos.publishEvent(new EstudianteConvocadoEvento(idPractica, idAutorizacion, idEstudiante, codigo));
        return idAutorizacion;
    }

    private void validarCaso(Long idCaso, int tiempoMaximo) {
        CasoProgramable caso = casosProgramables.consultar(idCaso);   // 404 si es un BORRADOR
        if (!caso.programable()) {
            throw new ReglaNegocioException("El caso está archivado: no se puede programar en nuevas sesiones");
        }
        validarTiempoDelCaso(caso, tiempoMaximo);
    }

    private static void validarTiempoDelCaso(CasoProgramable caso, int tiempoMaximo) {
        Integer estimado = caso.tiempoEstimadoMinutos();
        if (estimado != null && tiempoMaximo > estimado) {
            throw new ReglaNegocioException("El tiempo máximo (" + tiempoMaximo
                    + " min) no puede superar el tiempo estimado del caso (" + estimado + " min)");
        }
    }

    private void exigirEstudianteActivo(Long idEstudiante) {
        Usuario usuario = usuarios.findById(idEstudiante)
                .orElseThrow(() -> new ReglaNegocioException("El usuario " + idEstudiante + " no existe"));
        if (usuario.getRol() != Rol.ESTUDIANTE || !usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario " + idEstudiante + " no es un estudiante activo");
        }
    }

    private Practica sesionDelDocente(Long idPractica, Long idDocente) {
        return sesiones.buscar(idPractica, idDocente)
                .orElseThrow(() -> new RecursoNoEncontradoException("La sesión no existe o no pertenece al docente"));
    }

    private ConvocadoDto convocado(Autorizacion a) {
        Optional<Usuario> u = usuarios.findById(a.idEstudiante());
        return new ConvocadoDto(a.id(), a.idEstudiante(), u.map(Usuario::getNombreCompleto).orElse(null),
                u.map(Usuario::getCorreoInstitucional).orElse(null), a.estado());
    }

    private AutorizacionEstudianteDto paraEstudiante(Autorizacion a, Practica p) {
        int usados = (int) participaciones.intentosDeAutorizacion(a.id()).stream()
                .filter(IntentoParticipacion::cuentaComoIntento).count();
        int permitidos = new DatosRepeticion(a.estado(), null, p.idDocente(), p.estado(), usados,
                repeticiones.contarRepeticiones(a.id())).intentosPermitidos();
        return new AutorizacionEstudianteDto(a.id(), p.id(), p.idCaso(), casos.resumen(p.idCaso()).nombre(),
                p.espacioAcademico(), p.fechaInicio(), p.fechaFin(), p.tiempoMaximoMinutos(), p.lugar(),
                p.mensajePersonalizado(), p.urlAcceso(), p.estado(), usados, permitidos);
    }
}
