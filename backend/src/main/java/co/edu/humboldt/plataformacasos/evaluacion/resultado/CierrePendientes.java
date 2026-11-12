package co.edu.humboldt.plataformacasos.evaluacion.resultado;

import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.humboldt.plataformacasos.evaluacion.contrato.ActorActual;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.AutorizacionRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.CatalogoAcademico;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.AccesoEvaluacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.EstadoParticipacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.MaquinaEstadoParticipacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.Participacion;
import co.edu.humboldt.plataformacasos.evaluacion.participacion.ParticipacionRepositorio;

/**
 * RF-24: cierra intentos en curso como incompletos y registra ausencias en NO_INICIADA.
 */
@Service
public class CierrePendientes {

    private final ParticipacionRepositorio participaciones;
    private final ResultadoRepositorio resultados;
    private final CatalogoAcademico catalogo;
    private final AccesoEvaluacion acceso;
    private final CalculoNota calculo;
    private final Clock clock;

    public CierrePendientes(
            ParticipacionRepositorio participaciones,
            ResultadoRepositorio resultados,
            CatalogoAcademico catalogo,
            AccesoEvaluacion acceso,
            CalculoNota calculo,
            Clock clock) {
        this.participaciones = participaciones;
        this.resultados = resultados;
        this.catalogo = catalogo;
        this.acceso = acceso;
        this.calculo = calculo;
        this.clock = clock;
    }

    @Transactional
    public EvaluacionPendientesRespuesta evaluar(Long practicaId) {
        ActorActual actor = acceso.exigirActor();
        acceso.exigirPermiso(actor, Permiso.CONSULTAR_PARTICIPACIONES);
        return cerrar(acceso.exigirPracticaDocente(practicaId, actor));
    }

    /**
     * RF-24 sin actor: lo invoca la aplicación cuando el módulo 3 finaliza una sesión (manual o
     * automáticamente), dentro de la misma transacción.
     */
    @Transactional
    public EvaluacionPendientesRespuesta evaluarAlFinalizarSesion(Long practicaId) {
        return cerrar(catalogo.buscarPractica(practicaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Práctica no encontrada.")));
    }

    private EvaluacionPendientesRespuesta cerrar(PracticaRef practica) {
        int incompletas = 0;
        int ausencias = 0;
        int omitidas = 0;
        for (AutorizacionRef autorizacion : catalogo.autorizacionesDePractica(practica.id())) {
            List<Participacion> existentes = participaciones.findAllByAutorizacionIdOrderByNumeroIntentoAsc(autorizacion.id());
            List<Participacion> enCurso = existentes.stream()
                    .filter(p -> p.getEstado() == EstadoParticipacion.EN_CURSO)
                    .toList();
            if (!enCurso.isEmpty()) {
                for (Participacion part : enCurso) {
                    MaquinaEstadoParticipacion.marcarIncompleta(part, OffsetDateTime.now(clock));
                    participaciones.save(part);
                    calculo.calcular(part);
                    incompletas++;
                }
                continue;
            }
            if (existentes.stream().anyMatch(Participacion::esTerminal)) {
                omitidas++;
                continue;
            }
            Participacion noIniciada = existentes.stream()
                    .filter(p -> p.getEstado() == EstadoParticipacion.NO_INICIADA)
                    .findFirst()
                    .orElse(null);
            if (noIniciada == null) {
                noIniciada = new Participacion();
                noIniciada.setPracticaId(practica.id());
                noIniciada.setEstudianteId(autorizacion.estudianteId());
                noIniciada.setAutorizacionId(autorizacion.id());
                noIniciada.setNumeroIntento(1);
                noIniciada = participaciones.save(noIniciada);
            }
            if (resultados.findByParticipacionId(noIniciada.getId()).isPresent()) {
                omitidas++;
                continue;
            }
            calculo.calcular(noIniciada);
            ausencias++;
        }
        return new EvaluacionPendientesRespuesta(incompletas, ausencias, omitidas);
    }
}
