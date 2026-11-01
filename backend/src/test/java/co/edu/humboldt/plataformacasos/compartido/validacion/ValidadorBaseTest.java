package co.edu.humboldt.plataformacasos.compartido.validacion;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorBaseTest {

    /** Validador de prueba que registra que fue llamado. */
    private static final class ValidadorFijo extends ValidadorBase<String> {
        private final String nombre;
        private final boolean aprueba;
        private final List<String> llamados;

        ValidadorFijo(String nombre, boolean aprueba, List<String> llamados) {
            this.nombre = nombre;
            this.aprueba = aprueba;
            this.llamados = llamados;
        }

        @Override
        protected ResultadoValidacion validarCondicion(String solicitud) {
            llamados.add(nombre);
            return aprueba ? ResultadoValidacion.exito() : ResultadoValidacion.rechazo("falla " + nombre);
        }
    }

    @Test
    void recorreTodaLaCadenaCuandoTodosAprueban() {
        List<String> llamados = new ArrayList<>();
        Validador<String> a = new ValidadorFijo("a", true, llamados);
        a.establecerSiguiente(new ValidadorFijo("b", true, llamados))
         .establecerSiguiente(new ValidadorFijo("c", true, llamados));

        ResultadoValidacion resultado = a.validar("solicitud");

        assertTrue(resultado.aprobado());
        assertEquals(List.of("a", "b", "c"), llamados);
    }

    @Test
    void seDetieneEnElPrimerRechazo() {
        List<String> llamados = new ArrayList<>();
        Validador<String> a = new ValidadorFijo("a", true, llamados);
        a.establecerSiguiente(new ValidadorFijo("b", false, llamados))
         .establecerSiguiente(new ValidadorFijo("c", true, llamados));

        ResultadoValidacion resultado = a.validar("solicitud");

        assertFalse(resultado.aprobado());
        assertEquals("falla b", resultado.mensaje());
        assertEquals(List.of("a", "b"), llamados);
    }
}
