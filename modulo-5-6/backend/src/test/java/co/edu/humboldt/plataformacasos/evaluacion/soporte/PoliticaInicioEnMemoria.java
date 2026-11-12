package co.edu.humboldt.plataformacasos.evaluacion.soporte;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.AutorizacionRef;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PoliticaInicioIntento;
import co.edu.humboldt.plataformacasos.evaluacion.contrato.PracticaRef;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * DOBLE DE PRUEBA del puerto PoliticaInicioIntento. Aplica la misma regla del módulo 4 (RF-22, política
 * "mitad"): la sesión debe estar EN_CURSO y se puede ingresar hasta la mitad del tiempo máximo contado
 * desde el inicio de la sesión, usando los datos del catálogo en memoria.
 */
public class PoliticaInicioEnMemoria implements PoliticaInicioIntento {

    private final CatalogoAcademicoEnMemoria catalogo;
    private final Clock clock;

    public PoliticaInicioEnMemoria(CatalogoAcademicoEnMemoria catalogo, Clock clock) {
        this.catalogo = catalogo;
        this.clock = clock;
    }

    @Override
    public void exigirInicioPermitido(Long autorizacionId) {
        AutorizacionRef autorizacion = catalogo.buscarAutorizacion(autorizacionId).orElseThrow();
        PracticaRef practica = catalogo.buscarPractica(autorizacion.practicaId()).orElseThrow();
        if (!PracticaRef.EN_CURSO.equals(practica.estado())) {
            throw new ReglaNegocioException("La sesión aún no ha sido iniciada por el docente");
        }
        OffsetDateTime limite = practica.fechaInicio().plusMinutes(practica.tiempoMaximoMinutos() / 2);
        if (OffsetDateTime.now(clock).isAfter(limite)) {
            throw new ReglaNegocioException("Ingreso tardío: ya pasó el límite para iniciar la participación");
        }
    }
}
