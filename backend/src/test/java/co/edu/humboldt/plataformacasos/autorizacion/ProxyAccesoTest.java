package co.edu.humboldt.plataformacasos.autorizacion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Pruebas unitarias del Proxy de acceso (RF-02) con un servicio de ejemplo. */
class ProxyAccesoTest {

    interface ServicioEjemplo {
        @RequierePermiso(Permiso.DESCARGAR_REPORTES)
        String descargar();
    }

    interface ServicioSinPermisoDeclarado {
        String operacion();
    }

    private final AtomicInteger llamadasReales = new AtomicInteger();
    private final ServicioEjemplo real = () -> {
        llamadasReales.incrementAndGet();
        return "reporte";
    };
    private final ServicioAutorizacion autorizacion = mock(ServicioAutorizacion.class);

    @Test
    void delegaAlServicioRealCuandoHayPermiso() {
        ServicioEjemplo protegido = ProxyAcceso.proteger(ServicioEjemplo.class, real, autorizacion);

        assertThat(protegido.descargar()).isEqualTo("reporte");
        verify(autorizacion).exigirPermiso(Permiso.DESCARGAR_REPORTES);
        assertThat(llamadasReales).hasValue(1);
    }

    @Test
    void noLlamaAlServicioRealSinPermiso() {
        doThrow(new AccesoDenegadoException()).when(autorizacion).exigirPermiso(Permiso.DESCARGAR_REPORTES);
        ServicioEjemplo protegido = ProxyAcceso.proteger(ServicioEjemplo.class, real, autorizacion);

        assertThatThrownBy(protegido::descargar).isInstanceOf(AccesoDenegadoException.class);
        assertThat(llamadasReales).hasValue(0);
    }

    @Test
    void seNiegaAProtegerUnServicioConMetodosSinPermisoDeclarado() {
        ServicioSinPermisoDeclarado sinPermiso = () -> "x";
        assertThatThrownBy(() -> ProxyAcceso.proteger(ServicioSinPermisoDeclarado.class, sinPermiso, autorizacion))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("@RequierePermiso");
    }

    @Test
    void propagaLasExcepcionesDelServicioRealSinEnvolverlas() {
        ServicioEjemplo falla = () -> {
            throw new IllegalArgumentException("dato inválido");
        };
        ServicioEjemplo protegido = ProxyAcceso.proteger(ServicioEjemplo.class, falla, autorizacion);

        assertThatThrownBy(protegido::descargar).isInstanceOf(IllegalArgumentException.class);
    }
}
