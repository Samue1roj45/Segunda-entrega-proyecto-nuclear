package com.unihumboldt.nuclear.evaluacion.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unihumboldt.nuclear.evaluacion.contrato.CatalogoAcademico;
import com.unihumboldt.nuclear.evaluacion.notificacion.NotificacionFacade;
import com.unihumboldt.nuclear.evaluacion.notificacion.ResumenEnvio;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.ParticipacionRepository;
import com.unihumboldt.nuclear.evaluacion.participacion.RespuestaSeleccionadaRepository;

@ExtendWith(MockitoExtension.class)
class EvaluacionFacadePublicacionTest {

    @Mock
    private ResultadoRepository resultados;
    @Mock
    private ParticipacionRepository participaciones;
    @Mock
    private RespuestaSeleccionadaRepository respuestas;
    @Mock
    private NotaOficialRepository notasOficiales;
    @Mock
    private CatalogoAcademico catalogo;
    @Mock
    private AccesoEvaluacion acceso;
    @Mock
    private NotificacionFacade notificaciones;
    @Mock
    private Clock clock;

    @Test
    void delegaPublicarCalificacionesSinRecalcularNotas() {
        EvaluacionFacade facade = new EvaluacionFacade(
                resultados, participaciones, respuestas, notasOficiales, catalogo, acceso, notificaciones, clock);
        ResumenEnvio esperado = new ResumenEnvio(1, 1, 0);
        when(notificaciones.publicarCalificaciones(1L)).thenReturn(esperado);

        assertEquals(esperado, facade.publicarCalificaciones(1L));
        verify(notificaciones).publicarCalificaciones(1L);
        verifyNoInteractions(resultados, notasOficiales);
    }
}
