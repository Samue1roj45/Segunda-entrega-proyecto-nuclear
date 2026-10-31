package com.unihumboldt.nuclear.evaluacion.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.contrato.PracticaRef;
import com.unihumboldt.nuclear.evaluacion.contrato.Rol;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.Participacion;
import com.unihumboldt.nuclear.evaluacion.resultado.Resultado;
import com.unihumboldt.nuclear.evaluacion.resultado.ResultadoRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionFacadeCalificacionTest {

    @Mock
    private NotificacionRepository repositorio;
    @Mock
    private ResultadoRepository resultados;
    @Mock
    private CatalogoAcademico catalogo;
    @Mock
    private AccesoEvaluacion acceso;
    @Mock
    private PublicadorEventos publicador;

    @Test
    void publicaCalificacionPorIntentoSinTocarLaNota() {
        NotificacionFacade facade = new NotificacionFacade(repositorio, resultados, catalogo, acceso, publicador);
        ActorActual docente = new ActorActual(5L, null, 5L, Rol.DOCENTE);
        PracticaRef practica = new PracticaRef(1L, "Practica 1", 50L, 5L, null, null, "EN_CURSO", "");
        Resultado propio = resultado(3L, 1L, 10L, new BigDecimal("50.00"), true);
        Resultado ajeno = resultado(4L, 9L, 11L, new BigDecimal("80.00"), false);
        when(acceso.exigirActor()).thenReturn(docente);
        when(acceso.exigirPracticaDocente(1L, docente)).thenReturn(practica);
        when(resultados.findAllByOrderByIdDesc()).thenReturn(List.of(propio, ajeno));
        when(publicador.publicar(any())).thenReturn(new ResumenEnvio(1, 0, 0));

        ResumenEnvio resumen = facade.publicarCalificaciones(1L);

        assertEquals(new ResumenEnvio(1, 0, 0), resumen);
        assertEquals(new BigDecimal("50.00"), propio.getNotaFinal());
        verify(publicador).publicar(new EventoAcademico(
                TipoNotificacion.CALIFICACION,
                "CALIFICACION:3",
                1L,
                10L,
                3L,
                "Calificacion publicada: Practica 1",
                "Se publico la calificacion del intento 1 de 'Practica 1'. Nota: 50.00 / 100."));
        verify(publicador, never()).publicar(new EventoAcademico(
                TipoNotificacion.CALIFICACION,
                "CALIFICACION:4",
                9L,
                11L,
                4L,
                "Calificacion publicada: Practica 1",
                "Se publico la calificacion del intento 1 de 'Practica 1'. Nota: 80.00 / 100."));
        verify(resultados, never()).save(any());
    }

    private static Resultado resultado(
            long participacionId, long practicaId, long estudianteId, BigDecimal nota, boolean deLaPractica) {
        Participacion p = mock(Participacion.class);
        when(p.getPracticaId()).thenReturn(practicaId);
        if (deLaPractica) {
            when(p.getId()).thenReturn(participacionId);
            when(p.getEstudianteId()).thenReturn(estudianteId);
            when(p.getNumeroIntento()).thenReturn(1);
        }
        Resultado r = new Resultado();
        r.setParticipacion(p);
        r.setNotaFinal(nota);
        r.setEscalaMaxima(100);
        return r;
    }
}
