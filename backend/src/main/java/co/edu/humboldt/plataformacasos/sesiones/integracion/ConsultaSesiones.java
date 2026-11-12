package co.edu.humboldt.plataformacasos.sesiones.integracion;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Autorizacion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de solo lectura que los módulos 3 y 4 ofrecen a los demás (lo usa el módulo 5 a través del
 * catálogo académico): sesiones, estudiantes autorizados e intentos permitidos (RF-04, RF-18, RF-25).
 * No tiene endpoint: quien lo use ya validó al usuario en su propio módulo.
 */
public interface ConsultaSesiones {

    Optional<Practica> sesion(Long idPractica);

    Optional<Autorizacion> autorizacion(Long idAutorizacion);

    List<Autorizacion> autorizacionesActivas(Long idPractica);

    /** RF-25: 1 intento + repeticiones excepcionales autorizadas. */
    int intentosPermitidos(Long idAutorizacion);
}
