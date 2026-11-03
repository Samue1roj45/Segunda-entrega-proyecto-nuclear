package co.edu.humboldt.plataformacasos.casos.servicio;

import co.edu.humboldt.plataformacasos.casos.dominio.CasoEstudio;
import co.edu.humboldt.plataformacasos.casos.dominio.EstadoCaso;
import co.edu.humboldt.plataformacasos.casos.dsl.CasoEstudioBuilder;
import co.edu.humboldt.plataformacasos.casos.servicio.dto.CasoDetalleDTO;
import co.edu.humboldt.plataformacasos.casos.validacion.CasoNoPublicableException;
import co.edu.humboldt.plataformacasos.casos.validacion.InformeValidacion;
import co.edu.humboldt.plataformacasos.casos.validacion.ValidadorDslCeps;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** La fachada se prueba con el validador real: solo se simula la obtención del caso. */
class FachadaPublicacionCasosTest {

    private static final Long ID = 1L;
    private final AccesoCasos acceso = mock(AccesoCasos.class);
    private final FachadaPublicacionCasos fachada = new FachadaPublicacionCasos(acceso, new ValidadorDslCeps());

    private static CasoEstudio casoValido() {
        return CasoEstudioBuilder.nuevoCaso(5L, "Caso", "D", "H", 30)
                .escenario("E", "Contexto")
                .pregunta("¿Qué?")
                .respuesta("A", "A", true, new BigDecimal("100"), "Porque...")
                .respuesta("B", "B", false, BigDecimal.ZERO, "Porque...")
                .rubrica("R", "D")
                .criterio("Criterio", List.of("P"), BigDecimal.ZERO, BigDecimal.TEN)
                .construir();
    }

    @Test
    @DisplayName("Publica un caso que cumple las reglas")
    void publicacionValida() {
        when(acceso.paraModificar(ID)).thenReturn(casoValido());

        CasoDetalleDTO publicado = fachada.publicar(ID);

        assertThat(publicado.estado()).isEqualTo(EstadoCaso.PUBLICADO);
        assertThat(publicado.fechaPublicacion()).isNotNull();
    }

    @Test
    @DisplayName("Rechaza un caso inválido con el informe y lo deja en BORRADOR")
    void publicacionInvalida() {
        CasoEstudio incompleto = CasoEstudio.nuevoBorrador(5L, null, null, null, null);
        when(acceso.paraModificar(ID)).thenReturn(incompleto);

        assertThatThrownBy(() -> fachada.publicar(ID))
                .isInstanceOfSatisfying(CasoNoPublicableException.class,
                        e -> assertThat(e.informe().incumplimientos()).hasSize(4));
        assertThat(incompleto.getEstado()).isEqualTo(EstadoCaso.BORRADOR);
        assertThat(incompleto.getFechaPublicacion()).isNull();
    }

    @Test
    @DisplayName("Publicar un caso ya publicado es un conflicto, no un error de validación")
    void publicarDosVeces() {
        CasoEstudio caso = casoValido();
        caso.publicar();
        when(acceso.paraModificar(ID)).thenReturn(caso);

        assertThatThrownBy(() -> fachada.publicar(ID)).isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("Validar informa sin cambiar el estado")
    void validarNoCambiaElEstado() {
        CasoEstudio caso = casoValido();
        when(acceso.paraConsultar(ID)).thenReturn(caso);

        InformeValidacion informe = fachada.validar(ID);

        assertThat(informe.valido()).isTrue();
        assertThat(caso.getEstado()).isEqualTo(EstadoCaso.BORRADOR);
    }

    @Test
    @DisplayName("Archivar requiere un caso publicado y conserva su contenido")
    void archivar() {
        CasoEstudio borrador = casoValido();
        when(acceso.paraModificar(ID)).thenReturn(borrador);

        assertThatThrownBy(() -> fachada.archivar(ID)).isInstanceOf(ConflictoException.class);

        borrador.publicar();
        CasoDetalleDTO archivado = fachada.archivar(ID);

        assertThat(archivado.estado()).isEqualTo(EstadoCaso.ARCHIVADO);
        assertThat(archivado.escenarios()).hasSize(1);
        assertThat(archivado.rubrica()).isNotNull();
    }
}
