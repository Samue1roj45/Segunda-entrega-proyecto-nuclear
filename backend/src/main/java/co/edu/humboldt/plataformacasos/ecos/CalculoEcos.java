package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.CasoCatalogo;
import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.Opcion;
import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.Pregunta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recalcula la nota igual que ComputeReport del juego (EcosEvaluationSubsystem.cpp), pero con las respuestas
 * recibidas y el banco de preguntas de SOMA. Usa BigDecimal: los puntos se guardan exactos, no como double.
 * <ul>
 *   <li>obtenido = puntos × crédito (Correct = 1, Partial = el crédito de la opción, Incorrect o sin responder = 0)</li>
 *   <li>nota = round(50 × Σobtenido / Σposible) / 10 (redondeo hacia arriba en .5, igual que Math.round)</li>
 *   <li>nivel: Superior desde 4,5; Alto desde 4,0; Básico desde 3,0; Bajo por debajo</li>
 *   <li>criterio: suma de los puntos de cada ítem que lo lista; fortaleza desde 80 %, mejora bajo 60 %</li>
 * </ul>
 */
public final class CalculoEcos {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CINCUENTA = BigDecimal.valueOf(50);
    private static final BigDecimal DIEZ = BigDecimal.TEN;

    private CalculoEcos() {
    }

    public record Item(String preguntaId, String opcionId, BigDecimal obtenido, BigDecimal posible) {
    }

    public record Criterio(String id, BigDecimal obtenido, BigDecimal posible, BigDecimal porcentaje, String tipo) {
    }

    public record Calculo(List<Item> items, BigDecimal puntaje, BigDecimal posible, BigDecimal nota, String nivel,
                          List<Criterio> criterios) {
    }

    /** @param respuestas pregunta → opción elegida (null = sin responder); debe tener todas las preguntas del caso */
    public static Calculo calcular(CasoCatalogo caso, Map<String, String> respuestas) {
        List<Item> items = new ArrayList<>();
        Map<String, BigDecimal> obtenidoPorCriterio = new LinkedHashMap<>();
        Map<String, BigDecimal> posiblePorCriterio = new LinkedHashMap<>();
        caso.criterios().forEach(c -> {
            obtenidoPorCriterio.put(c, BigDecimal.ZERO);
            posiblePorCriterio.put(c, BigDecimal.ZERO);
        });
        BigDecimal suma = BigDecimal.ZERO;
        BigDecimal posible = BigDecimal.ZERO;
        for (Pregunta p : caso.preguntas()) {
            String elegida = respuestas.get(p.id());
            BigDecimal obtenido = p.puntos().multiply(credito(p, elegida));
            items.add(new Item(p.id(), elegida, obtenido.setScale(2, RoundingMode.HALF_UP), p.puntos()));
            suma = suma.add(obtenido);
            posible = posible.add(p.puntos());
            for (String c : p.criterios()) {
                obtenidoPorCriterio.merge(c, obtenido, BigDecimal::add);
                posiblePorCriterio.merge(c, p.puntos(), BigDecimal::add);
            }
        }
        BigDecimal nota = posible.signum() == 0 ? BigDecimal.ZERO.setScale(1)
                : suma.multiply(CINCUENTA).divide(posible, 10, RoundingMode.HALF_UP)
                        .setScale(0, RoundingMode.HALF_UP).divide(DIEZ, 1, RoundingMode.UNNECESSARY);

        List<Criterio> criterios = new ArrayList<>();
        for (String id : caso.criterios()) {
            BigDecimal o = obtenidoPorCriterio.get(id);
            BigDecimal pos = posiblePorCriterio.get(id);
            BigDecimal porcentaje = pos.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                    : o.multiply(CIEN).divide(pos, 2, RoundingMode.HALF_UP);
            criterios.add(new Criterio(id, o.setScale(2, RoundingMode.HALF_UP), pos, porcentaje, tipo(o, pos)));
        }
        return new Calculo(items, suma.setScale(2, RoundingMode.HALF_UP), posible, nota, nivel(nota), criterios);
    }

    private static BigDecimal credito(Pregunta p, String elegida) {
        if (elegida == null) {
            return BigDecimal.ZERO;
        }
        Opcion o = p.opciones().get(elegida);
        return switch (o.grado()) {
            case "Correct" -> BigDecimal.ONE;
            case "Partial" -> o.credito();
            default -> BigDecimal.ZERO;
        };
    }

    /** Se compara con los valores exactos, no con el porcentaje ya redondeado. */
    private static String tipo(BigDecimal obtenido, BigDecimal posible) {
        if (posible.signum() == 0) {
            return "NINGUNO";
        }
        BigDecimal cien = obtenido.multiply(CIEN);
        if (cien.compareTo(posible.multiply(BigDecimal.valueOf(80))) >= 0) {
            return "FORTALEZA";
        }
        if (cien.compareTo(posible.multiply(BigDecimal.valueOf(60))) < 0) {
            return "MEJORA";
        }
        return "NINGUNO";
    }

    static String nivel(BigDecimal nota) {
        if (nota.compareTo(new BigDecimal("4.5")) >= 0) {
            return "Superior";
        }
        if (nota.compareTo(new BigDecimal("4.0")) >= 0) {
            return "Alto";
        }
        if (nota.compareTo(new BigDecimal("3.0")) >= 0) {
            return "Básico";
        }
        return "Bajo";
    }
}
