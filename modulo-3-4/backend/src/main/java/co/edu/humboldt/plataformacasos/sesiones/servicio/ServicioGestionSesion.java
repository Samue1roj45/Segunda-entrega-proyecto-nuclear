package co.edu.humboldt.plataformacasos.sesiones.servicio;

import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import co.edu.humboldt.plataformacasos.sesiones.dto.SesionDto;
import co.edu.humboldt.plataformacasos.sesiones.estado.EstadoSesion;
import co.edu.humboldt.plataformacasos.sesiones.estado.EstadosSesion;
import co.edu.humboldt.plataformacasos.sesiones.evento.SesionFinalizadaEvento;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.SesionRepositorio;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/*
 #REQUISITO RF-18 || RF-19
 Patrón: STATE (contexto)
 Para qué sirve: consulta las sesiones del docente y gobierna su ciclo de vida
 SIN_INICIAR -> EN_CURSO -> FINALIZADO delegando en el objeto de estado actual. Al finalizar
 (manual o automáticamente) publica SesionFinalizadaEvento: el módulo 5 cierra los intentos
 abiertos, registra ausencias y califica (RF-24) en la misma transacción.
 */
@Service
public class ServicioGestionSesion {

    private static final Set<String> ESTADOS = Set.of(Practica.SIN_INICIAR, Practica.EN_CURSO, Practica.FINALIZADO);

    private final SesionRepositorio repo;
    private final ContenidoCasoInterno casos;
    private final ApplicationEventPublisher eventos;

    public ServicioGestionSesion(SesionRepositorio repo, ContenidoCasoInterno casos,
                                 ApplicationEventPublisher eventos) {
        this.repo = repo;
        this.casos = casos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<SesionDto> listar(Long idDocente, String estado) {
        String filtro = null;
        if (estado != null && !estado.isBlank()) {
            filtro = estado.trim().toUpperCase();
            if (!ESTADOS.contains(filtro)) {
                throw new ReglaNegocioException("Estado inválido. Use SIN_INICIAR, EN_CURSO o FINALIZADO");
            }
        }
        return repo.listarPorDocente(idDocente, filtro).stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public SesionDto obtener(Long idPractica, Long idDocente) {
        return aDto(cargar(idPractica, idDocente));
    }

    /** RF-19: inicia la sesión si su estado actual lo permite. */
    @Transactional
    public SesionDto iniciar(Long idPractica, Long idDocente) {
        EstadoSesion estado = EstadosSesion.de(cargar(idPractica, idDocente).estado());
        estado.iniciar(repo, idPractica, idDocente);
        return obtener(idPractica, idDocente);
    }

    /** RF-19: finaliza manualmente la sesión si su estado actual lo permite. */
    @Transactional
    public SesionDto finalizar(Long idPractica, Long idDocente) {
        EstadoSesion estado = EstadosSesion.de(cargar(idPractica, idDocente).estado());
        estado.finalizar(repo, idPractica, idDocente);
        eventos.publishEvent(new SesionFinalizadaEvento(idPractica, "MANUAL"));
        return obtener(idPractica, idDocente);
    }

    /** RF-19: cierre AUTOMÁTICO de las sesiones EN_CURSO cuyo plazo terminó. Devuelve cuántas cerró. */
    @Transactional
    public int cerrarVencidas() {
        int cerradas = 0;
        for (Practica vencida : repo.vencidasEnCurso()) {
            if (repo.cerrarAutomaticamente(vencida.id())) {
                eventos.publishEvent(new SesionFinalizadaEvento(vencida.id(), "AUTOMATICO"));
                cerradas++;
            }
        }
        return cerradas;
    }

    private Practica cargar(Long idPractica, Long idDocente) {
        return repo.buscar(idPractica, idDocente)
                .orElseThrow(() -> new RecursoNoEncontradoException("La sesión no existe o no pertenece al docente"));
    }

    private SesionDto aDto(Practica p) {
        return new SesionDto(p.id(), p.idCaso(), casos.resumen(p.idCaso()).nombre(), p.espacioAcademico(),
                p.fechaInicio(), p.fechaFin(), p.tiempoMaximoMinutos(), p.lugar(), p.mensajePersonalizado(),
                p.urlAcceso(), p.estado(), p.fechaInicioReal(), p.fechaCierreReal(), p.tipoCierre(),
                EstadosSesion.de(p.estado()).accionesPermitidas());
    }
}
