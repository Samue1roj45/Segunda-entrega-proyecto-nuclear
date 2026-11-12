package co.edu.humboldt.plataformacasos.sesiones.modulo4.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.sesiones.dto.SesionDto;
import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.sesiones.evento.SesionFinalizadaEvento;
import co.edu.humboldt.plataformacasos.sesiones.soporte.Datos;
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
 MÓDULO 4 · ESCENARIOS POSITIVOS · RF-19 (estados de la sesión) 4
 Patrón probado: State (SesionSinIniciar, SesionEnCurso) y su contexto ServicioGestionSesion.
 */
class EstadosSesionPositivaTest {

    @Test
    @DisplayName("CP-RF19-01 Una sesión SIN_INICIAR se puede iniciar")
    void CP_RF19_01_sesionSinIniciarSePuedeIniciar() {
        AccionesSesion acciones = mock(AccionesSesion.class);

        new SesionSinIniciar().iniciar(acciones, 1L, 2L);

        verify(acciones).iniciarEnBd(1L, 2L);
    }

    @Test
    @DisplayName("CP-RF19-03 Una sesión EN_CURSO se puede finalizar")
    void CP_RF19_03_sesionEnCursoSePuedeFinalizar() {
        AccionesSesion acciones = mock(AccionesSesion.class);

        new SesionEnCurso().finalizar(acciones, 1L, 2L);

        verify(acciones).finalizarEnBd(1L, 2L);
    }

    @Test
    @DisplayName("CP-RF19-05 El docente inicia su sesión y queda EN_CURSO")
    void CP_RF19_05_docenteIniciaSuSesion() {
        SesionRepositorio repo = mock(SesionRepositorio.class);
        ContenidoCasoInterno casos = mock(ContenidoCasoInterno.class);
        when(casos.resumen(1L)).thenReturn(Datos.casoPublicado(2L));
        when(repo.buscar(1L, 2L)).thenReturn(Optional.of(Datos.practica("SIN_INICIAR")),
                Optional.of(Datos.practica("EN_CURSO")));

        SesionDto r = new ServicioGestionSesion(repo, casos, mock(ApplicationEventPublisher.class)).iniciar(1L, 2L);

        verify(repo).iniciarEnBd(1L, 2L);
        assertThat(r.estado()).isEqualTo("EN_CURSO");
        assertThat(r.accionesPermitidas()).containsExactly("FINALIZAR");
    }

    @Test
    @DisplayName("CP-RF19-07 El cierre automático invoca el cierre de sesiones vencidas")
    void CP_RF19_07_cierreAutomaticoInvocaElCierre() {
        ServicioGestionSesion gestion = mock(ServicioGestionSesion.class);

        new TareaCierreAutomatico(gestion).cerrarVencidas();

        verify(gestion).cerrarVencidas();
    }

    @Test
    @DisplayName("CP-RF19-09 Al finalizar la sesión se avisa al módulo de evaluación (Observer)")
    void CP_RF19_09_finalizarPublicaElEvento() {
        SesionRepositorio repo = mock(SesionRepositorio.class);
        ContenidoCasoInterno casos = mock(ContenidoCasoInterno.class);
        ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
        when(casos.resumen(1L)).thenReturn(Datos.casoPublicado(2L));
        when(repo.buscar(1L, 2L)).thenReturn(Optional.of(Datos.practica("EN_CURSO")),
                Optional.of(Datos.practica("FINALIZADO")));

        SesionDto r = new ServicioGestionSesion(repo, casos, eventos).finalizar(1L, 2L);

        verify(repo).finalizarEnBd(1L, 2L);
        verify(eventos).publishEvent(new SesionFinalizadaEvento(1L, "MANUAL"));
        assertThat(r.accionesPermitidas()).isEmpty();
    }
}
