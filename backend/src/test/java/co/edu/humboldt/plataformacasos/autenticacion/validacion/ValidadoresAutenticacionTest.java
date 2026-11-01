package co.edu.humboldt.plataformacasos.autenticacion.validacion;

import co.edu.humboldt.plataformacasos.autenticacion.dominio.IntentoAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.MensajesAutenticacion;
import co.edu.humboldt.plataformacasos.autenticacion.dominio.SolicitudAutenticacion;
import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de cada eslabón de la cadena de autenticación (RF-01). */
class ValidadoresAutenticacionTest {

    private static final String DOMINIO = "institucion.edu.co";

    @Nested
    class CamposObligatorios {

        private final ValidadorCamposObligatorios validador = new ValidadorCamposObligatorios();

        @Test
        void aceptaCorreoYCodigo() {
            assertThat(validador.validar(new SolicitudAutenticacion("ana@x.co", "COD")).aprobado()).isTrue();
        }

        @ParameterizedTest
        @CsvSource(value = {
                "NULL, NULL, " + MensajesAutenticacion.CAMPOS_OBLIGATORIOS,
                "'  ', COD, " + MensajesAutenticacion.CORREO_OBLIGATORIO,
                "ana@x.co, '', " + MensajesAutenticacion.CODIGO_OBLIGATORIO
        }, nullValues = "NULL")
        void rechazaCamposVacios(String correo, String codigo, String mensajeEsperado) {
            ResultadoValidacion resultado = validador.validar(new SolicitudAutenticacion(correo, codigo));
            assertThat(resultado.aprobado()).isFalse();
            assertThat(resultado.mensaje()).isEqualTo(mensajeEsperado);
        }

        @Test
        void rechazaCodigoDemasiadoLargo() {
            String codigoLargo = "X".repeat(ValidadorCamposObligatorios.LONGITUD_MAXIMA_CODIGO + 1);
            assertThat(validador.validar(new SolicitudAutenticacion("ana@x.co", codigoLargo)).mensaje())
                    .isEqualTo(MensajesAutenticacion.CAMPO_DEMASIADO_LARGO);
        }
    }

    @Nested
    class DominioInstitucional {

        private final ValidadorDominioInstitucional validador = new ValidadorDominioInstitucional(DOMINIO);

        @ParameterizedTest
        @CsvSource({"ana.perez@institucion.edu.co", "  Ana.Perez@INSTITUCION.edu.co  "})
        void aceptaCorreosDelDominio(String correo) {
            assertThat(validador.validar(new SolicitudAutenticacion(correo, "COD")).aprobado()).isTrue();
        }

        @ParameterizedTest
        @CsvSource({"ana.perez@gmail.com", "ana@institucion.edu.co.falso.com", "@institucion.edu.co", "sin-arroba"})
        void rechazaCorreosDeOtroDominio(String correo) {
            ResultadoValidacion resultado = validador.validar(new SolicitudAutenticacion(correo, "COD"));
            assertThat(resultado.aprobado()).isFalse();
            assertThat(resultado.mensaje()).isEqualTo(MensajesAutenticacion.correoNoInstitucional(DOMINIO));
        }

        @Test
        void exigeDominioConfigurado() {
            assertThatThrownBy(() -> new ValidadorDominioInstitucional(" "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class EstudianteRegistrado {

        private final ValidadorEstudianteRegistrado validador = new ValidadorEstudianteRegistrado();

        @Test
        void aceptaEstudianteActivo() {
            Usuario ana = Usuario.nuevoEstudiante("ana@institucion.edu.co", "Ana");
            assertThat(validador.validar(new IntentoAutenticacion(Optional.of(ana), "COD")).aprobado()).isTrue();
        }

        @Test
        void rechazaCorreoNoRegistradoConMensajeGenerico() {
            ResultadoValidacion resultado = validador.validar(new IntentoAutenticacion(Optional.empty(), "COD"));
            assertThat(resultado.mensaje()).isEqualTo(MensajesAutenticacion.CREDENCIALES_INVALIDAS);
        }

        @Test
        void rechazaUnDocenteQueIntentaEntrarComoEstudiante() {
            Usuario docente = Usuario.nuevoDocente("doc@institucion.edu.co", "Docente");
            assertThat(validador.validar(new IntentoAutenticacion(Optional.of(docente), "COD")).aprobado()).isFalse();
        }

        @Test
        void rechazaEstudianteInactivo() {
            Usuario inactivo = mock(Usuario.class);
            when(inactivo.getRol()).thenReturn(Rol.ESTUDIANTE);
            when(inactivo.isActivo()).thenReturn(false);
            assertThat(validador.validar(new IntentoAutenticacion(Optional.of(inactivo), "COD")).aprobado()).isFalse();
        }
    }

    @Nested
    class CodigoAutorizacion {

        private static final long ID_ANA = 4L;

        private final ValidadorCodigoAutorizacion validador = new ValidadorCodigoAutorizacion(
                (estudianteId, codigo) -> estudianteId == ID_ANA && codigo.equals("CODIGO_VALIDO_01"));

        private Usuario ana() {
            Usuario ana = mock(Usuario.class);
            when(ana.getId()).thenReturn(ID_ANA);
            return ana;
        }

        @Test
        void aceptaElCodigoDelEstudianteSinImportarMayusculasNiEspacios() {
            IntentoAutenticacion intento = new IntentoAutenticacion(Optional.of(ana()), "  codigo_valido_01 ");
            assertThat(validador.validar(intento).aprobado()).isTrue();
        }

        @Test
        void rechazaCodigoInvalidoConMensajeGenerico() {
            ResultadoValidacion resultado = validador.validar(
                    new IntentoAutenticacion(Optional.of(ana()), "CODIGO_INVALIDO_99"));
            assertThat(resultado.aprobado()).isFalse();
            assertThat(resultado.mensaje()).isEqualTo(MensajesAutenticacion.CREDENCIALES_INVALIDAS);
        }

        @Test
        void rechazaSiNoHayEstudiante() {
            assertThat(validador.validar(new IntentoAutenticacion(Optional.empty(), "CODIGO_VALIDO_01")).aprobado())
                    .isFalse();
        }
    }
}
