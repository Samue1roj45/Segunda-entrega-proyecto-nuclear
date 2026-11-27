package co.edu.humboldt.plataformacasos.usuarios.administracion;

import java.util.List;

/**
 * Puerto del módulo de usuarios para saber qué registros académicos o de comunicación dependen de una
 * cuenta. Lo implementa el paquete de integración, que conoce las tablas de los otros módulos; así este
 * módulo no depende de ellos (inversión de dependencias).
 */
public interface RegistrosHistoricosUsuario {

    /** Solo los tipos con al menos un registro. Vacío si la cuenta no tiene historial que conservar. */
    List<RegistroHistorico> de(Long idUsuario);
}
