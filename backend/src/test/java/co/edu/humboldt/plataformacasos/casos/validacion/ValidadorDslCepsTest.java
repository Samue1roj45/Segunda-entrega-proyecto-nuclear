package co.edu.humboldt.plataformacasos.casos.validacion;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.CriterioEvaluacion;
import co.edu.humboldt.plataformacasos.casos.dominio.Respuesta;
import co.edu.humboldt.plataformacasos.casos.dsl.CasoEstudioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Cada prueba verifica la lista exacta de reglas: una regla no debe dispararse por error de otra. */
class ValidadorDslCepsTest {

    private static final Long DOCENTE = 1L;
    private final ValidadorDslCeps validador = new ValidadorDslCeps();

    private static CasoEstudioBuilder conEscenarioVacio(String contexto) {
        return CasoEstudioBuilder.nuevoCaso(DOCENTE, "Caso", "Desarrollo", "Historia", 30)
                .escenario("Escenario", contexto);
    }

    private static CasoEstudioBuilder conPregunta() {
        return conEscenarioVacio("Contexto").pregunta("¿Qué hacer?");
    }

    private static CasoEstudioBuilder respuestasValidas(CasoEstudioBuilder builder) {
        return builder.respuesta("A", "Opción A", true, new BigDecimal("100"), "Es correcta porque...")
                .respuesta("B", "Opción B", false, BigDecimal.ZERO, "Es incorrecta porque...");
    }

    private static CasoEstudioBuilder rubricaValida(CasoEstudioBuilder builder) {
        return builder.rubrica("Rúbrica", "Descripción")
                .criterio("Toma de decisiones", List.of("Pertinencia"), BigDecimal.ZERO, new BigDecimal("5"));
    }

    private static CasoEstudio casoValido() {
        return rubricaValida(respuestasValidas(conPregunta())).construir();
    }

    private List<String> reglasIncumplidas(CasoEstudio caso) {
        return validador.validar(caso).incumplimientos().stream().map(Incumplimiento::regla).toList();
    }

    @Test
    @DisplayName("Un caso completo cumple todas las reglas")
    void casoCompleto() {
        InformeValidacion informe = validador.validar(casoValido());

        assertThat(informe.valido()).isTrue();
        assertThat(informe.incumplimientos()).isEmpty();
    }

    @Test
    @DisplayName("Informa todas las reglas incumplidas, no solo la primera")
    void acumulaIncumplimientos() {
        CasoEstudio vacio = CasoEstudio.nuevoBorrador(DOCENTE, null, null, null, null);

        InformeValidacion informe = validador.validar(vacio);

        assertThat(informe.valido()).isFalse();
        assertThat(reglasIncumplidas(vacio)).containsExactly("OCL-01", "OCL-02", "OCL-03", "OCL-09");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("OCL-01: el nombre no puede estar vacío")
    void ocl01(String nombre) {
        CasoEstudio caso = casoValido();
        caso.actualizarDatos(nombre, "Desarrollo", "Historia", 30);

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-01");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -5})
    @DisplayName("OCL-02: el tiempo estimado debe ser positivo")
    void ocl02(Integer minutos) {
        CasoEstudio caso = casoValido();
        caso.actualizarDatos("Caso", "Desarrollo", "Historia", minutos);

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-02");
    }

    @Test
    @DisplayName("OCL-03: el caso debe tener al menos un escenario")
    void ocl03() {
        CasoEstudio caso = rubricaValida(CasoEstudioBuilder.nuevoCaso(DOCENTE, "Caso", "D", "H", 30)).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-03");
    }

    @Test
    @DisplayName("OCL-04: el escenario debe tener contexto")
    void ocl04Contexto() {
        CasoEstudio caso = rubricaValida(respuestasValidas(conEscenarioVacio(null).pregunta("¿Qué?"))).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-04");
    }

    @Test
    @DisplayName("OCL-04: el escenario debe tener al menos una pregunta")
    void ocl04Preguntas() {
        CasoEstudio caso = rubricaValida(conEscenarioVacio("Contexto")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-04");
    }

    @Test
    @DisplayName("OCL-05: la pregunta debe tener al menos dos opciones")
    void ocl05() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "Opción", true, new BigDecimal("100"), "Porque...")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-05");
    }

    @Test
    @DisplayName("OCL-06: la opción debe tener justificación")
    void ocl06Justificacion() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "A", true, new BigDecimal("100"), "Porque...")
                .respuesta("B", "B", false, BigDecimal.ZERO, "  ")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-06");
    }

    @Test
    @DisplayName("OCL-06: la opción debe declarar si es correcta")
    void ocl06Declaracion() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "A", true, new BigDecimal("100"), "Porque...")
                .respuesta("B", "B", null, BigDecimal.ZERO, "Porque...")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-06");
    }

    @Test
    @DisplayName("OCL-07: una pregunta sin respuesta correcta se rechaza")
    void ocl07SinCorrecta() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "A", false, BigDecimal.ZERO, "Porque...")
                .respuesta("B", "B", false, BigDecimal.ZERO, "Porque...")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-07");
    }

    @Test
    @DisplayName("OCL-07: una pregunta con dos respuestas correctas se rechaza")
    void ocl07DosCorrectas() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "A", true, new BigDecimal("100"), "Porque...")
                .respuesta("B", "B", true, new BigDecimal("100"), "Porque...")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-07");
    }

    @Test
    @DisplayName("OCL-08: los identificadores no se repiten, sin distinguir mayúsculas")
    void ocl08Repetidos() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "A", true, new BigDecimal("100"), "Porque...")
                .respuesta("a", "B", false, BigDecimal.ZERO, "Porque...")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-08");
    }

    @Test
    @DisplayName("OCL-08: el mismo identificador en otra pregunta sí es válido")
    void ocl08OtraPregunta() {
        CasoEstudio caso = rubricaValida(respuestasValidas(respuestasValidas(conPregunta()).pregunta("¿Otra?")))
                .construir();

        assertThat(reglasIncumplidas(caso)).isEmpty();
    }

    @Test
    @DisplayName("OCL-08: toda opción debe tener identificador")
    void ocl08Faltante() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "A", true, new BigDecimal("100"), "Porque...")
                .respuesta(null, "B", false, BigDecimal.ZERO, "Porque...")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-08");
    }

    @Test
    @DisplayName("OCL-09: el caso debe tener rúbrica")
    void ocl09() {
        CasoEstudio caso = respuestasValidas(conPregunta()).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-09");
    }

    @Test
    @DisplayName("OCL-10: la rúbrica debe tener al menos un criterio")
    void ocl10SinCriterios() {
        CasoEstudio caso = respuestasValidas(conPregunta()).rubrica("Rúbrica", "D").construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-10");
    }

    @Test
    @DisplayName("OCL-10: un criterio sin rango completo se rechaza")
    void ocl10RangoIncompleto() {
        CasoEstudio caso = respuestasValidas(conPregunta()).rubrica("Rúbrica", "D")
                .criterio("Criterio", List.of(), BigDecimal.ZERO, null).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-10");
    }

    @Test
    @DisplayName("OCL-10: min >= max se rechaza aunque el dato haya llegado a la entidad por otra vía")
    void ocl10MinNoMenorQueMax() {
        CasoEstudio caso = casoValido();
        CriterioEvaluacion criterio = caso.getRubrica().getCriterios().get(0);
        ReflectionTestUtils.setField(criterio, "rangoMin", new BigDecimal("5"));

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-10");
    }

    @Test
    @DisplayName("OCL-11: la opción debe tener porcentaje")
    void ocl11SinPorcentaje() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "A", true, new BigDecimal("100"), "Porque...")
                .respuesta("B", "B", false, null, "Porque...")).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-11");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "100.01"})
    @DisplayName("OCL-11: un porcentaje fuera de 0 a 100 se rechaza aunque haya llegado por otra vía")
    void ocl11FueraDeRango(String porcentaje) {
        CasoEstudio caso = casoValido();
        Respuesta respuesta = caso.getEscenarios().get(0).getPreguntas().get(0).getRespuestas().get(0);
        ReflectionTestUtils.setField(respuesta, "porcentaje", new BigDecimal(porcentaje));

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-11");
    }

    @Test
    @DisplayName("OCL-12: el criterio no puede estar vacío")
    void ocl12Vacio() {
        CasoEstudio caso = respuestasValidas(conPregunta()).rubrica("Rúbrica", "D")
                .criterio(" ", List.of(), BigDecimal.ZERO, BigDecimal.TEN).construir();

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-12");
    }

    @Test
    @DisplayName("OCL-12: el criterio no puede superar 150 caracteres aunque haya llegado por otra vía")
    void ocl12Largo() {
        CasoEstudio caso = casoValido();
        CriterioEvaluacion criterio = caso.getRubrica().getCriterios().get(0);
        ReflectionTestUtils.setField(criterio, "criterio", "x".repeat(151));

        assertThat(reglasIncumplidas(caso)).containsExactly("OCL-12");
    }

    @Test
    @DisplayName("Cada incumplimiento indica la sección y el lugar que se debe corregir")
    void ubicacionDelIncumplimiento() {
        CasoEstudio caso = rubricaValida(conPregunta()
                .respuesta("A", "A", true, new BigDecimal("100"), "Porque...")
                .respuesta("B", "B", false, BigDecimal.ZERO, null)).construir();

        Incumplimiento incumplimiento = validador.validar(caso).incumplimientos().get(0);

        assertThat(incumplimiento.seccion()).isEqualTo(SeccionCaso.ESCENARIOS);
        assertThat(incumplimiento.ubicacion()).isEqualTo("Escenario 1 «Escenario» › Pregunta 1 › Opción B");
    }
}
