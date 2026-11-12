package co.edu.humboldt.plataformacasos.sesiones.modulo4.positivas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Herramienta;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SolicitudRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.TipoHerramienta;
import co.edu.humboldt.plataformacasos.sesiones.dto.HerramientaDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.IntentosDto;
import co.edu.humboldt.plataformacasos.sesiones.dto.RegistrarHerramientaRequest;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.HerramientasRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.RepeticionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioHerramientas;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.servicio.repeticion.CadenaValidacionRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.soporte.Datos;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
/*
 MÓDULO 4 · ESCENARIOS POSITIVOS · RF-25 (repetición excepcional) y RF-28 (herramientas profesionales) 6
 (El registro de inicio y fin, RF-12, es de los módulos 5 y 6 y se prueba allí.)
 */
class RegistroRepeticionHerramientasPositivaTest {

    private static final Herramienta CALCULADORA = new Herramienta(1L, 1L, "Calculadora de puntajes",
            TipoHerramienta.CALCULADORA, "Suma los puntajes del test", "{\"operaciones\":[\"+\"]}", 1);

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
        RepeticionRepositorio repo = mock(RepeticionRepositorio.class);
        when(repo.cargar(5L)).thenReturn(
                Optional.of(Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 1, 0)),
                Optional.of(Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 1, 1)));

        IntentosDto r = new ServicioRepeticion(repo, new CadenaValidacionRepeticion())
                .autorizar(5L, 2L, "Falla de conexión");

        assertThat(r.intentosPermitidos()).isEqualTo(2);
        verify(repo).autorizar(5L, 2L, "Falla de conexión");
    }

    @Test
    @DisplayName("CP-RF25-05 El docente de la sesión consulta los intentos usados y permitidos")
    void CP_RF25_05_consultaLosIntentos() {
        RepeticionRepositorio repo = mock(RepeticionRepositorio.class);
        when(repo.cargar(5L)).thenReturn(Optional.of(Datos.datosRepeticion("ACTIVA", 2L, 8L, "EN_CURSO", 1, 1)));

        IntentosDto r = new ServicioRepeticion(repo, new CadenaValidacionRepeticion()).consultar(5L, 8L);

        assertThat(r.intentosUsados()).isEqualTo(1);
        assertThat(r.intentosPermitidos()).isEqualTo(2);
    }

    // ---------- RF-28 ----------
    @Test
    @DisplayName("CP-RF28-01 El estudiante en curso consulta las herramientas definidas para el caso")
    void CP_RF28_01_consultaLasHerramientasDelCaso() {
        HerramientasRepositorio repoHerramientas = mock(HerramientasRepositorio.class);
        ParticipacionEstudianteRepositorio participaciones = mock(ParticipacionEstudianteRepositorio.class);
        when(participaciones.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(participaciones.estadoParticipacion(7L)).thenReturn("EN_CURSO");
        when(participaciones.idCasoDeParticipacion(7L)).thenReturn(1L);
        when(repoHerramientas.delCaso(1L)).thenReturn(List.of(CALCULADORA));

        List<HerramientaDto> r = new ServicioHerramientas(repoHerramientas, participaciones,
                mock(ContenidoCasoInterno.class)).consultar(3L, 7L);

        assertThat(r).extracting(HerramientaDto::nombre).containsExactly("Calculadora de puntajes");
    }

    @Test
    @DisplayName("CP-RF28-03 Un caso sin herramientas definidas devuelve una lista vacía")
    void CP_RF28_03_casoSinHerramientasDevuelveListaVacia() {
        HerramientasRepositorio repoHerramientas = mock(HerramientasRepositorio.class);
        ParticipacionEstudianteRepositorio participaciones = mock(ParticipacionEstudianteRepositorio.class);
        when(participaciones.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(participaciones.estadoParticipacion(7L)).thenReturn("EN_CURSO");
        when(participaciones.idCasoDeParticipacion(7L)).thenReturn(1L);
        when(repoHerramientas.delCaso(1L)).thenReturn(List.of());

        assertThat(new ServicioHerramientas(repoHerramientas, participaciones, mock(ContenidoCasoInterno.class))
                .consultar(3L, 7L)).isEmpty();
    }

    @Test
    @DisplayName("CP-RF28-05 El docente creador del caso registra una herramienta")
    void CP_RF28_05_creadorRegistraHerramienta() {
        HerramientasRepositorio repoHerramientas = mock(HerramientasRepositorio.class);
        ContenidoCasoInterno casos = mock(ContenidoCasoInterno.class);
        when(casos.resumen(1L)).thenReturn(Datos.casoPublicado(2L));
        when(repoHerramientas.registrar(any(Herramienta.class))).thenReturn(9L);

        HerramientaDto r = new ServicioHerramientas(repoHerramientas, mock(ParticipacionEstudianteRepositorio.class),
                casos).registrar(2L, 1L, new RegistrarHerramientaRequest("Tabla de baremos", TipoHerramienta.TABLA,
                null, "{\"filas\":[]}", null));

        assertThat(r.idHerramienta()).isEqualTo(9L);
        assertThat(r.orden()).isEqualTo(1);
    }
}
