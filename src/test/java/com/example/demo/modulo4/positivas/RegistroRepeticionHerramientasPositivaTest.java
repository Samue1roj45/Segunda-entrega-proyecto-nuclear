package com.example.demo.modulo4.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.dto.IntentosDto;
import com.example.demo.dto.RegistroTiempoDto;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// MÓDULO 4 · ESCENARIOS POSITIVOS · RF-12 (inicio y fin), RF-25 (repetición excepcional) y RF-28 (herramientas) 6
// Patrón probado: Chain of Responsibility (validación de la repetición).
class RegistroRepeticionHerramientasPositivaTest {

    private static final LocalDateTime T0 = Datos.AHORA;
    private static final String RUTA = "$.herramientas";

    private final RegistroTiempoDto registro = new RegistroTiempoDto(7L, 1L, 3L, "Estudiante Uno", "FINALIZADA",
            T0, T0.plusMinutes(20), 1200L, T0.minusMinutes(5), null, null);

    // ---------- RF-12 ----------
    @Test
    @DisplayName("CP-RF12-01 El estudiante consulta el inicio y fin de su participación")
    void CP_RF12_01_estudianteConsultaSuRegistro() {
        RegistroTiempoRepository repoRegistro = mock(RegistroTiempoRepository.class);
        ParticipacionRepository participaciones = mock(ParticipacionRepository.class);
        when(participaciones.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(repoRegistro.porParticipacion(7L)).thenReturn(Optional.of(registro));

        RegistroTiempoDto r = new RegistroTiempoService(repoRegistro, participaciones).consultarPropio(3L, 7L);

        assertThat(r.duracionSegundos()).isEqualTo(1200L);
        assertThat(r.horaFin()).isEqualTo(T0.plusMinutes(20));
    }

    @Test
    @DisplayName("CP-RF12-03 El docente consulta los registros de su sesión")
    void CP_RF12_03_docenteConsultaLosRegistrosDeSuSesion() {
        RegistroTiempoRepository repoRegistro = mock(RegistroTiempoRepository.class);
        when(repoRegistro.porSesion(1L, 2L)).thenReturn(List.of(registro));

        List<RegistroTiempoDto> r = new RegistroTiempoService(repoRegistro,
                mock(ParticipacionRepository.class)).listarSesion(1L, 2L);

        assertThat(r).containsExactly(registro);
    }

    // ---------- RF-25 ----------
    @Test
    @DisplayName("CP-RF25-01 La cadena acepta una repetición pedida por el docente creador")
    void CP_RF25_01_cadenaAceptaAlDocenteCreador() {
        SolicitudRepeticion s = new SolicitudRepeticion(5L, 2L, "Falla de conexión",
                Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 1, 0));

        assertThatCode(() -> new CadenaValidacionRepeticion().validar(s)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-RF25-03 Autorizar la repetición concede un intento adicional")
    void CP_RF25_03_autorizaUnIntentoAdicional() {
        RepeticionRepository repo = mock(RepeticionRepository.class);
        when(repo.cargar(5L)).thenReturn(
                Optional.of(Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 1, 0)),
                Optional.of(Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 1, 1)));

        IntentosDto r = new RepeticionService(repo, new CadenaValidacionRepeticion())
                .autorizar(5L, 2L, "Falla de conexión");

        assertThat(r.intentosPermitidos()).isEqualTo(2);
        verify(repo).autorizar(5L, 2L, "Falla de conexión");
    }

    // ---------- RF-28 ----------
    @Test
    @DisplayName("CP-RF28-01 El estudiante en curso consulta las herramientas definidas para el caso")
    void CP_RF28_01_consultaLasHerramientasDelCaso() {
        HerramientasRepository repoHerramientas = mock(HerramientasRepository.class);
        ParticipacionRepository participaciones = mock(ParticipacionRepository.class);
        when(participaciones.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(repoHerramientas.estadoParticipacion(7L)).thenReturn("EN_CURSO");
        when(repoHerramientas.herramientasDelCaso(7L, RUTA))
                .thenReturn(Optional.of("[{\"id\":\"calculadora\"}]"));

        String json = new HerramientasService(repoHerramientas, participaciones, RUTA).consultar(3L, 7L);

        assertThat(json).contains("calculadora");
    }

    @Test
    @DisplayName("CP-RF28-03 Un caso sin herramientas definidas devuelve una lista vacía")
    void CP_RF28_03_casoSinHerramientasDevuelveListaVacia() {
        HerramientasRepository repoHerramientas = mock(HerramientasRepository.class);
        ParticipacionRepository participaciones = mock(ParticipacionRepository.class);
        when(participaciones.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(repoHerramientas.estadoParticipacion(7L)).thenReturn("EN_CURSO");
        when(repoHerramientas.herramientasDelCaso(7L, RUTA)).thenReturn(Optional.empty());

        String json = new HerramientasService(repoHerramientas, participaciones, RUTA).consultar(3L, 7L);

        assertThat(json).isEqualTo("[]");
    }
}
