package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import org.springframework.stereotype.Component;

/** Regla común de los contratos: un BORRADOR no es visible para otros módulos. */
@Component
class CasosYaPublicados {

    private final CasoEstudioRepositorio casos;

    CasosYaPublicados(CasoEstudioRepositorio casos) {
        this.casos = casos;
    }

    CasoEstudio exigir(Long idCaso) {
        return casos.findByIdAndEstadoNot(idCaso, EstadoCaso.BORRADOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("El caso no existe o no ha sido publicado."));
    }
}
