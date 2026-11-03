package com.unihumboldt.nuclear.evaluacion.resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unihumboldt.nuclear.evaluacion.notificacion.NotificacionFacade;
import com.unihumboldt.nuclear.evaluacion.notificacion.ResumenEnvio;

@ExtendWith(MockitoExtension.class)
class EvaluacionFacadePublicacionTest {

    @Mock
    private CalculoNota calculo;
    @Mock
    private LecturaResultados lectura;
    @Mock
    private NotaOficialServicio notasOficiales;
    @Mock
    private CierrePendientes pendientes;
    @Mock
    private NotificacionFacade notificaciones;

    @Test
    void delegaPublicarCalificacionesSinRecalcularNotas() {
        EvaluacionFacade facade = new EvaluacionFacade(calculo, lectura, notasOficiales, pendientes, notificaciones);
        ResumenEnvio esperado = new ResumenEnvio(1, 1, 0);
        when(notificaciones.publicarCalificaciones(1L)).thenReturn(esperado);

        assertEquals(esperado, facade.publicarCalificaciones(1L));
        verify(notificaciones).publicarCalificaciones(1L);
        verifyNoInteractions(calculo, lectura, notasOficiales, pendientes);
    }
}
