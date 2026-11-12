package co.edu.humboldt.plataformacasos.sesiones.modulo4.negativas;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import org.springframework.context.ApplicationEventPublisher;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.SesionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.tareas.TareaCierreAutomatico;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioGestionSesion;
import co.edu.humboldt.plataformacasos.sesiones.estado.AccionesSesion;
import co.edu.humboldt.plataformacasos.sesiones.estado.SesionEnCurso;
import co.edu.humboldt.plataformacasos.sesiones.estado.SesionSinIniciar;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
/*
 MÓDULO 4 · ESCENARIOS NEGATIVOS · RF-19 (estados de la sesión) 4
 Pareja exacta de EstadosSesionPositivaTest.
 */
class EstadosSesionNegativaTest {

    @Test
    @DisplayName("CP-RF19-02 Una sesión SIN_INICIAR no se puede finalizar")
    void CP_RF19_02_sesionSinIniciarNoSePuedeFinalizar() {
        AccionesSesion acciones = mock(AccionesSesion.class);

        assertThatThrownBy(() -> new SesionSinIniciar().finalizar(acciones, 1L, 2L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("aún no se ha iniciado");
        verify(acciones, never()).finalizarEnBd(anyLong(), anyLong());
    }

    @Test
    @DisplayName("CP-RF19-04 Una sesión EN_CURSO no se puede iniciar de nuevo")
    void CP_RF19_04_sesionEnCursoNoSePuedeIniciarDeNuevo() {
        AccionesSesion acciones = mock(AccionesSesion.class);

        assertThatThrownBy(() -> new SesionEnCurso().iniciar(acciones, 1L, 2L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya está en curso");
        verify(acciones, never()).iniciarEnBd(anyLong(), anyLong());
    }

    @Test
    @DisplayName("CP-RF19-06 No se puede iniciar una sesión que no existe o es de otro docente")
    void CP_RF19_06_noIniciaUnaSesionInexistente() {
        SesionRepositorio repo = mock(SesionRepositorio.class);
        when(repo.buscar(99L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ServicioGestionSesion(repo, mock(ContenidoCasoInterno.class),
                mock(ApplicationEventPublisher.class)).iniciar(99L, 2L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");
        verify(repo, never()).iniciarEnBd(anyLong(), anyLong());
    }

    @Test
    @DisplayName("CP-RF19-08 Si el cierre automático falla, el error no detiene la aplicación")
    void CP_RF19_08_falloDelCierreAutomaticoNoSePropaga() {
        ServicioGestionSesion gestion = mock(ServicioGestionSesion.class);
        doThrow(new RuntimeException("BD no disponible")).when(gestion).cerrarVencidas();

        assertThatCode(() -> new TareaCierreAutomatico(gestion).cerrarVencidas()).doesNotThrowAnyException();
    }
}
