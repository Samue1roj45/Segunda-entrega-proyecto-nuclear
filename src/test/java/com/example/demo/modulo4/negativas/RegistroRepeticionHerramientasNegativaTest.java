package com.example.demo.modulo4.negativas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.exception.ReglaNegocioException;
import com.example.demo.model.SolicitudRepeticion;
import com.example.demo.test.Datos;
import com.example.demo.repository.HerramientasRepository;
import com.example.demo.repository.ParticipacionRepository;
import com.example.demo.repository.RegistroTiempoRepository;
import com.example.demo.repository.RepeticionRepository;
import com.example.demo.service.HerramientasService;
import com.example.demo.service.RegistroTiempoService;
import com.example.demo.service.RepeticionService;
import com.example.demo.service.repeticion.CadenaValidacionRepeticion;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
/*
 MÓDULO 4 · ESCENARIOS NEGATIVOS · RF-12 (inicio y fin), RF-25 (repetición excepcional) y RF-28 (herramientas) 6
 Pareja exacta de RegistroRepeticionHerramientasPositivaTest.
 */
class RegistroRepeticionHerramientasNegativaTest {

    private static final String RUTA = "$.herramientas";

    // ---------- RF-12 ----------
    @Test
    @DisplayName("CP-RF12-02 Un estudiante ajeno no puede ver el registro de otra participación")
    void CP_RF12_02_ajenoNoVeElRegistro() {
        RegistroTiempoRepository repoRegistro = mock(RegistroTiempoRepository.class);
        ParticipacionRepository participaciones = mock(ParticipacionRepository.class);
        when(participaciones.participacionDeEstudiante(7L, 4L)).thenReturn(false);

        assertThatThrownBy(() -> new RegistroTiempoService(repoRegistro, participaciones).consultarPropio(4L, 7L))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(repoRegistro, never()).porParticipacion(anyLong());
    }

    @Test
    @DisplayName("CP-RF12-04 Rechazar la consulta cuando la participación no tiene registro")
    void CP_RF12_04_rechazaParticipacionSinRegistro() {
        RegistroTiempoRepository repoRegistro = mock(RegistroTiempoRepository.class);
        ParticipacionRepository participaciones = mock(ParticipacionRepository.class);
        when(participaciones.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(repoRegistro.porParticipacion(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new RegistroTiempoService(repoRegistro, participaciones).consultarPropio(3L, 7L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no existe");
    }

    // ---------- RF-25 ----------
    @Test
    @DisplayName("CP-RF25-02 La cadena rechaza la repetición pedida por un docente que no creó el caso")
    void CP_RF25_02_cadenaRechazaAlDocenteNoCreador() {
        SolicitudRepeticion s = new SolicitudRepeticion(5L, 9L, "Intento no válido",
                Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 1, 0));

        assertThatThrownBy(() -> new CadenaValidacionRepeticion().validar(s))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("creador");
    }

    @Test
    @DisplayName("CP-RF25-04 Un docente ajeno no puede conceder el intento adicional")
    void CP_RF25_04_docenteAjenoNoConcedeElIntento() {
        RepeticionRepository repo = mock(RepeticionRepository.class);
        when(repo.cargar(5L)).thenReturn(Optional.of(Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 1, 0)));

        assertThatThrownBy(() -> new RepeticionService(repo, new CadenaValidacionRepeticion())
                .autorizar(5L, 9L, "Intento no válido"))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(repo, never()).autorizar(anyLong(), anyLong(), any());
    }

    // ---------- RF-28 ----------
    @Test
    @DisplayName("CP-RF28-02 Un estudiante ajeno no puede ver las herramientas de otra participación")
    void CP_RF28_02_ajenoNoVeLasHerramientas() {
        HerramientasRepository repoHerramientas = mock(HerramientasRepository.class);
        ParticipacionRepository participaciones = mock(ParticipacionRepository.class);
        when(participaciones.participacionDeEstudiante(7L, 4L)).thenReturn(false);

        assertThatThrownBy(() -> new HerramientasService(repoHerramientas, participaciones, RUTA).consultar(4L, 7L))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(repoHerramientas, never()).herramientasDelCaso(anyLong(), anyString());
    }

    @Test
    @DisplayName("CP-RF28-04 Las herramientas no se entregan cuando la participación ya terminó")
    void CP_RF28_04_noEntregaHerramientasConLaParticipacionTerminada() {
        HerramientasRepository repoHerramientas = mock(HerramientasRepository.class);
        ParticipacionRepository participaciones = mock(ParticipacionRepository.class);
        when(participaciones.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(repoHerramientas.estadoParticipacion(7L)).thenReturn("FINALIZADA");

        assertThatThrownBy(() -> new HerramientasService(repoHerramientas, participaciones, RUTA).consultar(3L, 7L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("en curso");
    }
}
