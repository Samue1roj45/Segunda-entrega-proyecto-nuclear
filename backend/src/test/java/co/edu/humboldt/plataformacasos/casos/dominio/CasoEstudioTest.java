package co.edu.humboldt.plataformacasos.casos.dominio;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CasoEstudioTest {

    private static final Long DOCENTE = 7L;

    private static CasoEstudio borrador() {
        return CasoEstudio.nuevoBorrador(DOCENTE, "Caso", "Desarrollo", "Historia", 30);
    }

    private static void conId(Object entidad, long id) {
        ReflectionTestUtils.setField(entidad, "id", id);
    }

    @Test
    @DisplayName("RF-09: todo caso nuevo nace en BORRADOR y pertenece a su docente")
    void nuevoEnBorrador() {
        CasoEstudio caso = borrador();

        assertThat(caso.getEstado()).isEqualTo(EstadoCaso.BORRADOR);
        assertThat(caso.perteneceA(DOCENTE)).isTrue();
        assertThat(caso.perteneceA(99L)).isFalse();
        assertThat(caso.getFechaCreacion()).isNotNull();
    }

    @Test
    @DisplayName("RF-05: un borrador se puede guardar completamente vacío")
    void borradorIncompleto() {
        assertThatCode(() -> CasoEstudio.nuevoBorrador(DOCENTE, null, null, null, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("RF-05: la edición normaliza los textos y cambia los datos")
    void edicion() {
        CasoEstudio caso = borrador();

        caso.actualizarDatos("  Otro nombre ", " ", "Otra historia", 45);

        assertThat(caso.getNombre()).isEqualTo("Otro nombre");
        assertThat(caso.getDesarrolloSituacional()).isNull();
        assertThat(caso.getContextoHistoria()).isEqualTo("Otra historia");
        assertThat(caso.getTiempoEstimadoMinutos()).isEqualTo(45);
    }

    @Test
    @DisplayName("Un nombre que no cabe en su columna se rechaza aun en borrador")
    void nombreDemasiadoLargo() {
        assertThatThrownBy(() -> CasoEstudio.nuevoBorrador(DOCENTE, "x".repeat(151), null, null, null))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("RF-06: los escenarios y preguntas conservan su orden y se pueden eliminar")
    void escenariosYPreguntas() {
        CasoEstudio caso = borrador();
        Escenario primero = caso.agregarEscenario("Uno", "Contexto");
        Escenario segundo = caso.agregarEscenario("Dos", "Contexto");
        conId(primero, 1L);
        conId(segundo, 2L);
        Pregunta pregunta = segundo.agregarPregunta("¿Qué hacer?");
        conId(pregunta, 10L);

        assertThat(primero.getOrden()).isEqualTo(1);
        assertThat(segundo.getOrden()).isEqualTo(2);
        assertThat(caso.pregunta(10L)).isSameAs(pregunta);

        segundo.eliminarPregunta(10L);
        caso.eliminarEscenario(1L);

        assertThat(segundo.getPreguntas()).isEmpty();
        assertThat(caso.getEscenarios()).containsExactly(segundo);
    }

    @Test
    @DisplayName("Un id que no es de este caso responde como no encontrado")
    void idAjeno() {
        CasoEstudio caso = borrador();

        assertThatThrownBy(() -> caso.escenario(404L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> caso.pregunta(404L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> caso.respuesta(404L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(caso::rubricaRegistrada).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("RF-07: marcar la correcta deja una sola opción correcta")
    void marcarCorrecta() {
        Pregunta pregunta = borrador().agregarEscenario("E", "C").agregarPregunta("¿Qué?");
        Respuesta a = pregunta.agregarRespuesta("A", "A", true, null, null);
        Respuesta b = pregunta.agregarRespuesta("B", "B", true, null, null);
        Respuesta c = pregunta.agregarRespuesta("C", "C", null, null, null);
        conId(a, 1L);
        conId(b, 2L);
        conId(c, 3L);

        pregunta.marcarCorrecta(3L);

        assertThat(a.getEsCorrecta()).isFalse();
        assertThat(b.getEsCorrecta()).isFalse();
        assertThat(c.getEsCorrecta()).isTrue();
    }

    @Test
    @DisplayName("RF-07: el código de la opción se normaliza a mayúsculas")
    void codigoEnMayusculas() {
        Pregunta pregunta = borrador().agregarEscenario("E", "C").agregarPregunta("¿Qué?");

        assertThat(pregunta.agregarRespuesta(" a ", "A", null, null, null).getCodigo()).isEqualTo("A");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01", "150"})
    @DisplayName("OCL-11: un porcentaje fuera de 0 a 100 se rechaza al guardar")
    void porcentajeFueraDeDominio(String valor) {
        Pregunta pregunta = borrador().agregarEscenario("E", "C").agregarPregunta("¿Qué?");

        assertThatThrownBy(() -> pregunta.agregarRespuesta("A", "A", true, new BigDecimal(valor), "J"))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("Un valor con más de dos decimales se rechaza en vez de redondearse en silencio")
    void precisionDecimal() {
        Pregunta pregunta = borrador().agregarEscenario("E", "C").agregarPregunta("¿Qué?");

        assertThatThrownBy(() -> pregunta.agregarRespuesta("A", "A", true, new BigDecimal("33.333"), "J"))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("RF-08: la justificación se escribe y se edita")
    void justificacion() {
        Respuesta respuesta = borrador().agregarEscenario("E", "C").agregarPregunta("¿Qué?")
                .agregarRespuesta("A", "A", true, BigDecimal.TEN, "Primera");

        respuesta.justificar("  Corregida ");

        assertThat(respuesta.getJustificacion()).isEqualTo("Corregida");
    }

    @Test
    @DisplayName("RF-17: un caso tiene una sola rúbrica, que se actualiza al registrarla de nuevo")
    void unaSolaRubrica() {
        CasoEstudio caso = borrador();

        Rubrica rubrica = caso.registrarRubrica("Primera", "D");
        Rubrica misma = caso.registrarRubrica("Segunda", "D");

        assertThat(misma).isSameAs(rubrica);
        assertThat(caso.getRubrica().getNombre()).isEqualTo("Segunda");

        caso.retirarRubrica();

        assertThat(caso.getRubrica()).isNull();
    }

    @Test
    @DisplayName("RF-17: los parámetros del criterio se conservan como lista")
    void parametrosDelCriterio() {
        CriterioEvaluacion criterio = borrador().registrarRubrica("R", "D")
                .agregarCriterio("Criterio", List.of(" Pertinencia ", "", "Justificación"), null, null);

        assertThat(criterio.getParametros()).containsExactly("Pertinencia", "Justificación");
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "6"})
    @DisplayName("OCL-10: min >= max se rechaza al guardar")
    void rangoInvalido(String min) {
        Rubrica rubrica = borrador().registrarRubrica("R", "D");

        assertThatThrownBy(() -> rubrica.agregarCriterio("C", List.of(), new BigDecimal(min), new BigDecimal("5")))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("RF-09: el ciclo de vida solo avanza de BORRADOR a PUBLICADO y de PUBLICADO a ARCHIVADO")
    void cicloDeVida() {
        CasoEstudio caso = borrador();

        assertThatThrownBy(caso::archivar).isInstanceOf(ConflictoException.class);

        caso.publicar();
        assertThat(caso.getEstado()).isEqualTo(EstadoCaso.PUBLICADO);
        assertThat(caso.getFechaPublicacion()).isNotNull();
        assertThatThrownBy(caso::publicar).isInstanceOf(ConflictoException.class);

        caso.archivar();
        assertThat(caso.getEstado()).isEqualTo(EstadoCaso.ARCHIVADO);
        assertThat(caso.getFechaArchivado()).isNotNull();
        assertThatThrownBy(caso::publicar).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(caso::archivar).isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("Solo un caso PUBLICADO está disponible para programar")
    void disponibilidad() {
        assertThat(EstadoCaso.BORRADOR.disponibleParaProgramar()).isFalse();
        assertThat(EstadoCaso.PUBLICADO.disponibleParaProgramar()).isTrue();
        assertThat(EstadoCaso.ARCHIVADO.disponibleParaProgramar()).isFalse();
    }

    @Test
    @DisplayName("RF-09: ningún elemento de un caso publicado se puede modificar")
    void contenidoPublicadoInmutable() {
        CasoEstudio caso = borrador();
        Escenario escenario = caso.agregarEscenario("E", "C");
        Pregunta pregunta = escenario.agregarPregunta("¿Qué?");
        Respuesta respuesta = pregunta.agregarRespuesta("A", "A", true, BigDecimal.TEN, "J");
        Rubrica rubrica = caso.registrarRubrica("R", "D");
        CriterioEvaluacion criterio = rubrica.agregarCriterio("C", List.of(), BigDecimal.ZERO, BigDecimal.ONE);
        caso.publicar();

        assertThatThrownBy(() -> caso.actualizarDatos("X", null, null, 1)).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> caso.agregarEscenario("X", "Y")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> escenario.actualizar("X", "Y")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> escenario.agregarPregunta("X")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> pregunta.actualizarEnunciado("X")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> pregunta.agregarRespuesta("B", "B", false, null, null))
                .isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> respuesta.justificar("X")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> caso.registrarRubrica("X", "Y")).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(caso::retirarRubrica).isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> rubrica.agregarCriterio("X", List.of(), null, null))
                .isInstanceOf(ConflictoException.class);
        assertThatThrownBy(() -> criterio.actualizar("X", List.of(), null, null))
                .isInstanceOf(ConflictoException.class);
    }
}
