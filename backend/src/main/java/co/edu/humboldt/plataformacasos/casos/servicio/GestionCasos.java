package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoResumen;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.DatosCasoRequest;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.util.List;

/** RF-05: estructura académica del caso. Protegida por el Proxy de acceso de RF-02. */
public interface GestionCasos {

    /** Casos del docente actual; {@code estado} es opcional. */
    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    List<CasoResumen> listar(EstadoCaso estado);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    CasoDetalleDTO crear(DatosCasoRequest datos);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    CasoDetalleDTO consultar(Long idCaso);

    @RequierePermiso(Permiso.GESTIONAR_CASOS)
    CasoDetalleDTO actualizar(Long idCaso, DatosCasoRequest datos);
}
