package co.edu.humboldt.plataformacasos.ecos;

import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.CasoCatalogo;
import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.Opcion;
import co.edu.humboldt.plataformacasos.ecos.ModeloEcos.Pregunta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Valores SINTÉTICOS: solo comprueban la fórmula del contrato (no son los puntos reales del juego). */
class CalculoEcosTest {

    private static Pregunta pregunta(String id, String puntos, List<String> criterios, Opcion... opciones) {
        Map<String, Opcion> mapa = new java.util.LinkedHashMap<>();
        for (Opcion o : opciones) { mapa.put(o.id(), o); }
        return new Pregunta(id, new BigDecimal(puntos), criterios, mapa);
    }

    private static Opcion ok(String id) { return new Opcion(id, "Correct", BigDecimal.ONE); }
    private static Opcion parcial(String id, String credito) { return new Opcion(id, "Partial", new BigDecimal(credito)); }
    private static Opcion mal(String id) { return new Opcion(id, "Incorrect", BigDecimal.ZERO); }

    private final CasoCatalogo caso = new CasoCatalogo("v", "CasoX", "Caso de prueba", List.of(
            pregunta("P1", "40", List.of("C1"), ok("A"), parcial("B", "0.5"), mal("C")),
            pregunta("P2", "30", List.of("C1", "C2"), ok("A"), parcial("B", "0.4"), mal("C")),
            pregunta("P3", "20", List.of("C2"), ok("A"), mal("B")),
            pregunta("P4", "10", List.of("C3"), ok("A"), mal("B"))), List.of("C1", "C2", "C3"));

    private static Map<String, String> r(String p1, String p2, String p3, String p4) {
        Map<String, String> m = new HashMap<>();
        m.put("P1", p1); m.put("P2", p2); m.put("P3", p3); m.put("P4", p4);
        return m;
    }

    @Test
    @DisplayName("Crédito parcial, pregunta sin responder y clasificación por criterio")
    void calculoMixto() {
        var c = CalculoEcos.calcular(caso, r("B", "A", null, "A"));  // 20 + 30 + 0 + 10
        assertThat(c.puntaje()).isEqualByComparingTo("60.00");
        assertThat(c.posible()).isEqualByComparingTo("100");
        assertThat(c.nota()).isEqualByComparingTo("3.0");
        assertThat(c.nivel()).isEqualTo("Básico");
        assertThat(c.criterios()).extracting(CalculoEcos.Criterio::id, CalculoEcos.Criterio::tipo)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("C1", "NINGUNO"),   // 50/70 = 71,43 %
                        org.assertj.core.groups.Tuple.tuple("C2", "NINGUNO"),             // 30/50 = 60 %
                        org.assertj.core.groups.Tuple.tuple("C3", "FORTALEZA"));          // 10/10
        assertThat(c.criterios().get(0).porcentaje()).isEqualByComparingTo("71.43");
        assertThat(c.items().get(2).opcionId()).isNull();
        assertThat(c.items().get(2).obtenido()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Máximo, mínimo y niveles en sus límites")
    void extremosYNiveles() {
        assertThat(CalculoEcos.calcular(caso, r("A", "A", "A", "A")).nota()).isEqualByComparingTo("5.0");
        assertThat(CalculoEcos.calcular(caso, r("A", "A", "A", "A")).nivel()).isEqualTo("Superior");
        var cero = CalculoEcos.calcular(caso, r(null, null, null, null));
        assertThat(cero.nota()).isEqualByComparingTo("0.0");
        assertThat(cero.nivel()).isEqualTo("Bajo");
        assertThat(cero.criterios()).allMatch(k -> k.tipo().equals("MEJORA"));
        assertThat(CalculoEcos.nivel(new BigDecimal("4.5"))).isEqualTo("Superior");
        assertThat(CalculoEcos.nivel(new BigDecimal("4.4"))).isEqualTo("Alto");
        assertThat(CalculoEcos.nivel(new BigDecimal("4.0"))).isEqualTo("Alto");
        assertThat(CalculoEcos.nivel(new BigDecimal("3.9"))).isEqualTo("Básico");
        assertThat(CalculoEcos.nivel(new BigDecimal("3.0"))).isEqualTo("Básico");
        assertThat(CalculoEcos.nivel(new BigDecimal("2.9"))).isEqualTo("Bajo");
    }

    @Test
    @DisplayName("El redondeo de la nota sube en .5, igual que Math.round del contrato")
    void redondeo() {
        CasoCatalogo uno = new CasoCatalogo("v", "X", "X", List.of(
                pregunta("P", "100", List.of("C1"), ok("A"), parcial("B", "0.71"))), List.of("C1"));
        var c = CalculoEcos.calcular(uno, Map.of("P", "B"));           // 71 puntos → 50 × 0,71 = 35,5 → 36 → 3,6
        assertThat(c.puntaje()).isEqualByComparingTo("71.00");
        assertThat(c.nota()).isEqualByComparingTo("3.6");
        assertThat(c.nivel()).isEqualTo("Básico");
    }

    @Test
    @DisplayName("Fortaleza desde el 80 % exacto y mejora por debajo del 60 %")
    void umbralesDeCriterio() {
        CasoCatalogo k = new CasoCatalogo("v", "X", "X", List.of(
                pregunta("P1", "50", List.of("C1"), ok("A"), parcial("B", "0.8"), parcial("D", "0.59")),
                pregunta("P2", "50", List.of("C2"), ok("A"), parcial("B", "0.8"), parcial("D", "0.59"))), List.of("C1", "C2"));
        var c = CalculoEcos.calcular(k, Map.of("P1", "B", "P2", "D"));
        assertThat(c.criterios().get(0).tipo()).isEqualTo("FORTALEZA");  // 80 %
        assertThat(c.criterios().get(1).tipo()).isEqualTo("MEJORA");     // 59 %
    }
}
