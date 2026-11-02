package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoEstudioRepositorio;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoResumen;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosCasoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ServicioCasos implements GestionCasos {

    private final CasoEstudioRepositorio casos;
    private final AccesoCasos acceso;

    public ServicioCasos(CasoEstudioRepositorio casos, AccesoCasos acceso) {
        this.casos = casos;
        this.acceso = acceso;
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
}
