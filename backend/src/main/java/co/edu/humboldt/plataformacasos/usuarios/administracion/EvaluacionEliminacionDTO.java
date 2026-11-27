package co.edu.humboldt.plataformacasos.usuarios.administracion;

import java.util.List;

/**
 * Resultado de evaluar si una cuenta se puede eliminar definitivamente. Si no se puede, {@code motivo}
 * explica por qué y {@code registros} lista el historial que impide borrarla.
 */
public record EvaluacionEliminacionDTO(boolean permitida, String motivo, List<RegistroHistorico> registros) {
}
