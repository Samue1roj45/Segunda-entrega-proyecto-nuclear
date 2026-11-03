package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Único lugar donde se resuelve un caso a partir de su id y se comprueba que pertenece al docente
 * de la petición. El permiso lo verifica antes el Proxy de RF-02; esto cubre la pertenencia del recurso.
 */
@Component
public class AccesoCasos {

    private final CasoEstudioRepositorio casos;
    private final ServicioAutorizacion autorizacion;

    public AccesoCasos(CasoEstudioRepositorio casos, ServicioAutorizacion autorizacion) {
        this.casos = casos;
        this.autorizacion = autorizacion;
    }

    public Long idDocenteActual() {
        return autorizacion.verificarIdentidad().id();
    }

    public CasoEstudio paraConsultar(Long idCaso) {
        return delDocenteActual(casos.findById(idCaso));
    }

    /** Debe llamarse dentro de una transacción: bloquea el caso hasta que esta termine. */
    public CasoEstudio paraModificar(Long idCaso) {
        return delDocenteActual(casos.buscarParaModificar(idCaso));
    }

    private CasoEstudio delDocenteActual(Optional<CasoEstudio> posibleCaso) {
        CasoEstudio caso = posibleCaso.orElseThrow(() -> new RecursoNoEncontradoException("El caso no existe."));
        if (!caso.perteneceA(idDocenteActual())) {
            throw new AccesoDenegadoException();
        }
        return caso;
    }
}
