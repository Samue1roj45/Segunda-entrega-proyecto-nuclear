package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.dsl.DocumentoDsl;
import co.edu.humboldt.plataformacasos.casos.dsl.ImportadorDsl;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoResumen;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosCasoRequest;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.ResultadoImportacionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ServicioCasos implements GestionCasos {

    private final CasoEstudioRepositorio casos;
    private final AccesoCasos acceso;
    private final ImportadorDsl importador;

    public ServicioCasos(CasoEstudioRepositorio casos, AccesoCasos acceso, ImportadorDsl importador) {
        this.casos = casos;
        this.acceso = acceso;
        this.importador = importador;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CasoResumen> listar(EstadoCaso estado) {
        return casos.listarDelDocente(acceso.idDocenteActual(), estado);
    }

    @Override
    public CasoDetalleDTO crear(DatosCasoRequest datos) {
        CasoEstudio caso = CasoEstudio.nuevoBorrador(acceso.idDocenteActual(), datos.nombre(),
                datos.desarrolloSituacional(), datos.contextoHistoria(), datos.tiempoEstimadoMinutos());
        return CasoDetalleDTO.de(casos.saveAndFlush(caso));
    }

    @Override
    @Transactional(readOnly = true)
    public CasoDetalleDTO consultar(Long idCaso) {
        return CasoDetalleDTO.de(acceso.paraConsultar(idCaso));
    }

    @Override
    public CasoDetalleDTO actualizar(Long idCaso, DatosCasoRequest datos) {
        CasoEstudio caso = acceso.paraModificar(idCaso);
        caso.actualizarDatos(datos.nombre(), datos.desarrolloSituacional(), datos.contextoHistoria(),
                datos.tiempoEstimadoMinutos());
        return CasoDetalleDTO.de(caso);
    }

    @Override
    public ResultadoImportacionDTO importarDsl(DocumentoDsl documento) {
        CasoEstudio caso = casos.saveAndFlush(importador.construir(acceso.idDocenteActual(), documento));
        return new ResultadoImportacionDTO(CasoDetalleDTO.de(caso), advertenciasDe(documento));
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoDsl exportarDsl(Long idCaso) {
        return DocumentoDsl.de(acceso.paraConsultar(idCaso));
    }

    /** Las herramientas son de RF-28, ajeno a este módulo: se avisa en vez de perderlas en silencio. */
    private static List<String> advertenciasDe(DocumentoDsl documento) {
        List<Object> herramientas = documento.caso().herramientas();
        if (herramientas == null || herramientas.isEmpty()) {
            return List.of();
        }
        return List.of("Se omitieron " + herramientas.size()
                + " herramienta(s) profesional(es): RF-28 todavía no está disponible en esta versión.");
    }
}
