package co.edu.humboldt.plataformacasos.casos.integracion;

import co.edu.humboldt.plataformacasos.autorizacion.RequierePermiso;
import co.edu.humboldt.plataformacasos.casos.repositorio.CasoProgramable;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.util.List;

// Patrón Puerto/contrato entre módulos: M3 consulta casos publicados de M2 sin conocer su persistencia.
/** Contrato para el Módulo 3 (RF-04, RF-18): qué casos se pueden programar. */
public interface ConsultaCasosProgramables {

    /** Solo casos PUBLICADOS: son los únicos disponibles para nuevas programaciones (RF-09). */
    @RequierePermiso(Permiso.PROGRAMAR_SESIONES)
    List<CasoProgramable> publicados();

    /** Un caso ARCHIVADO se devuelve con {@code programable = false}; un BORRADOR responde 404. */
    @RequierePermiso(Permiso.PROGRAMAR_SESIONES)
    CasoProgramable consultar(Long idCaso);
}
