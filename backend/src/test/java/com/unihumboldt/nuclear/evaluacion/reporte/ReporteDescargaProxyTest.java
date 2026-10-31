package com.unihumboldt.nuclear.evaluacion.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.unihumboldt.nuclear.evaluacion.contrato.ActorActual;
import com.unihumboldt.nuclear.evaluacion.contrato.Rol;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoDenegadoException;
import com.unihumboldt.nuclear.evaluacion.participacion.AccesoEvaluacion;
import com.unihumboldt.nuclear.evaluacion.participacion.EstadoParticipacion;

@ExtendWith(MockitoExtension.class)
class ReporteDescargaProxyTest {

    @Mock
    private ReporteFacade real;

    @Mock
    private AccesoEvaluacion acceso;

    private ReporteDescargaProxy proxy;

    @BeforeEach
    void setup() {
        proxy = new ReporteDescargaProxy(real, acceso);
    }

    @Test
    void estudianteNoDescargaReporteAjeno() {
        when(acceso.exigirActor()).thenReturn(new ActorActual(10L, 10L, null, Rol.ESTUDIANTE));
        assertThrows(AccesoDenegadoException.class, () -> proxy.pdf(1L, 11L));
        verify(real, never()).filas(any(), any());
        verify(real, never()).pdfDe(any());
    }

    @Test
    void docenteAjenoNoDescargaPractica() {
        ActorActual ajeno = new ActorActual(99L, null, 99L, Rol.DOCENTE);
        when(acceso.exigirActor()).thenReturn(ajeno);
        org.mockito.Mockito.doThrow(new AccesoDenegadoException("No autorizado para esta práctica."))
                .when(acceso).exigirPracticaDocente(1L, ajeno);

        assertThrows(AccesoDenegadoException.class, () -> proxy.excel(1L, null));
        verify(real, never()).filas(anyLong(), any());
    }

    @Test
    void docentePropioRecibeFilasVisiblesSinRecalcularNota() {
        ActorActual docente = new ActorActual(5L, null, 5L, Rol.DOCENTE);
        FilaReporteEvaluacion propia = fila(1L, 10L, new BigDecimal("0.00"), "AUSENCIA");
        FilaReporteEvaluacion ajena = fila(9L, 11L, new BigDecimal("80.00"), "FINALIZADA");
        ArchivoReporte archivo = new ArchivoReporte("r.pdf", ReporteFacade.PDF, new byte[] {1});
        when(acceso.exigirActor()).thenReturn(docente);
        when(real.filas(1L, null)).thenReturn(List.of(propia, ajena));
        when(acceso.visible(docente, 1L, 10L)).thenReturn(true);
        when(acceso.visible(docente, 9L, 11L)).thenReturn(false);
        when(real.pdfDe(List.of(propia))).thenReturn(archivo);

        assertEquals(archivo, proxy.pdf(1L, null));
        verify(acceso).exigirPracticaDocente(1L, docente);
        verify(real).pdfDe(List.of(propia));
        assertEquals(new BigDecimal("0.00"), propia.notaFinal());
    }

    private static FilaReporteEvaluacion fila(long practicaId, long estudianteId, BigDecimal nota, String clase) {
        return new FilaReporteEvaluacion(
                1L, 2L, practicaId, "P", estudianteId, "a@b.c", "N", 1,
                EstadoParticipacion.NO_INICIADA, clase,
                0, 0, 2, 0, 4, 100, nota, false, nota, null, null);
    }
}
