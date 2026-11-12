package co.edu.humboldt.plataformacasos.sesiones.modulo4.negativas;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.humboldt.plataformacasos.casos.integracion.ContenidoCasoInterno;
import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.ReglaNegocioException;
import co.edu.humboldt.plataformacasos.sesiones.dominio.Herramienta;
import co.edu.humboldt.plataformacasos.sesiones.dominio.SolicitudRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.dominio.TipoHerramienta;
import co.edu.humboldt.plataformacasos.sesiones.dto.RegistrarHerramientaRequest;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.HerramientasRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.ParticipacionEstudianteRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.repositorio.RepeticionRepositorio;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioHerramientas;
import co.edu.humboldt.plataformacasos.sesiones.servicio.ServicioRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.servicio.repeticion.CadenaValidacionRepeticion;
import co.edu.humboldt.plataformacasos.sesiones.soporte.Datos;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
/*
 MÓDULO 4 · ESCENARIOS NEGATIVOS · RF-25 (repetición excepcional) y RF-28 (herramientas profesionales) 6
 Pareja exacta de RegistroRepeticionHerramientasPositivaTest.
 */
class RegistroRepeticionHerramientasNegativaTest {

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
        RepeticionRepositorio repo = mock(RepeticionRepositorio.class);
        when(repo.cargar(5L)).thenReturn(Optional.of(Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 1, 0)));

        assertThatThrownBy(() -> new ServicioRepeticion(repo, new CadenaValidacionRepeticion())
                .autorizar(5L, 9L, "Intento no válido"))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(repo, never()).autorizar(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("CP-RF25-06 La repetición es excepcional: no se concede una segunda (máximo dos intentos)")
    void CP_RF25_06_noSeConcedeUnaSegundaRepeticion() {
        SolicitudRepeticion s = new SolicitudRepeticion(5L, 2L, "Otra vez",
                Datos.datosRepeticion("ACTIVA", 2L, 2L, "EN_CURSO", 2, 1));

        assertThatThrownBy(() -> new CadenaValidacionRepeticion().validar(s))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("máximo dos intentos");
    }

    // ---------- RF-28 ----------
    @Test
    @DisplayName("CP-RF28-02 Un estudiante ajeno no puede ver las herramientas de otra participación")
    void CP_RF28_02_ajenoNoVeLasHerramientas() {
        HerramientasRepositorio repoHerramientas = mock(HerramientasRepositorio.class);
        ParticipacionEstudianteRepositorio participaciones = mock(ParticipacionEstudianteRepositorio.class);
        when(participaciones.participacionDeEstudiante(7L, 4L)).thenReturn(false);

        assertThatThrownBy(() -> new ServicioHerramientas(repoHerramientas, participaciones,
                mock(ContenidoCasoInterno.class)).consultar(4L, 7L))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(repoHerramientas, never()).delCaso(anyLong());
    }

    @Test
    @DisplayName("CP-RF28-04 Las herramientas no se entregan cuando la participación ya terminó")
    void CP_RF28_04_noEntregaHerramientasConLaParticipacionTerminada() {
        HerramientasRepositorio repoHerramientas = mock(HerramientasRepositorio.class);
        ParticipacionEstudianteRepositorio participaciones = mock(ParticipacionEstudianteRepositorio.class);
        when(participaciones.participacionDeEstudiante(7L, 3L)).thenReturn(true);
        when(participaciones.estadoParticipacion(7L)).thenReturn("FINALIZADA");

        assertThatThrownBy(() -> new ServicioHerramientas(repoHerramientas, participaciones,
                mock(ContenidoCasoInterno.class)).consultar(3L, 7L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("en curso");
    }

    @Test
    @DisplayName("CP-RF28-06 Un docente que no creó el caso no puede registrar herramientas")
    void CP_RF28_06_noCreadorNoRegistraHerramientas() {
        HerramientasRepositorio repoHerramientas = mock(HerramientasRepositorio.class);
        ContenidoCasoInterno casos = mock(ContenidoCasoInterno.class);
        when(casos.resumen(1L)).thenReturn(Datos.casoPublicado(2L));

        assertThatThrownBy(() -> new ServicioHerramientas(repoHerramientas,
                mock(ParticipacionEstudianteRepositorio.class), casos).registrar(9L, 1L,
                new RegistrarHerramientaRequest("Tabla", TipoHerramienta.TABLA, null, null, null)))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("creador");
        verify(repoHerramientas, never()).registrar(any(Herramienta.class));
    }
}
