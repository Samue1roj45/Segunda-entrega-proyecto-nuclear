package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.compartido.validacion.ResultadoValidacion;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cadenas de archivo y eliminación de cuentas, eslabón por eslabón, sin base de datos. */
class CadenasAdministracionCuentasTest {

    private static final long ACTOR = 1L;

    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final RegistrosHistoricosUsuario registros = mock(RegistrosHistoricosUsuario.class);
    private final CadenasAdministracionCuentas cadenas = new CadenasAdministracionCuentas(usuarios, registros);

    private static Usuario cuenta(long id, Rol rol) {
        Usuario u = Usuario.nuevoDocente("u" + id + "@institucion.edu.co", "Usuario " + id);
        ReflectionTestUtils.setField(u, "id", id);
        ReflectionTestUtils.setField(u, "rol", rol);
        return u;
    }

    @Test
    @DisplayName("Nadie archiva ni elimina su propia cuenta")
    void cuentaPropia() {
        Usuario yo = cuenta(ACTOR, Rol.ADMINISTRADOR);
        ResultadoValidacion archivo = cadenas.archivo().validar(new SolicitudCambioCuenta(yo, ACTOR));
        ResultadoValidacion eliminacion = cadenas.eliminacion().validar(new SolicitudCambioCuenta(yo, ACTOR));
        assertThat(archivo.aprobado()).isFalse();
        assertThat(archivo.mensaje()).isEqualTo(MensajesAdministracion.CUENTA_PROPIA);
        assertThat(eliminacion.mensaje()).isEqualTo(MensajesAdministracion.CUENTA_PROPIA);
    }

    @Test
    @DisplayName("El último administrador activo no se archiva ni se elimina; si hay otro, sí")
    void ultimoAdministrador() {
        Usuario otro = cuenta(2L, Rol.ADMINISTRADOR);
        when(usuarios.bloquearAdministradoresActivos()).thenReturn(List.of(otro));
        assertThat(cadenas.archivo().validar(new SolicitudCambioCuenta(otro, ACTOR)).mensaje())
                .isEqualTo(MensajesAdministracion.ULTIMO_ADMINISTRADOR);

        when(usuarios.bloquearAdministradoresActivos()).thenReturn(List.of(otro, cuenta(3L, Rol.ADMINISTRADOR)));
        assertThat(cadenas.archivo().validar(new SolicitudCambioCuenta(otro, ACTOR)).aprobado()).isTrue();
    }

    @Test
    @DisplayName("Un administrador archivado se puede eliminar sin contar como administrador activo")
    void administradorArchivado() {
        Usuario archivado = cuenta(2L, Rol.ADMINISTRADOR);
        archivado.archivar();
        when(registros.de(anyLong())).thenReturn(List.of());
        assertThat(cadenas.eliminacion().validar(new SolicitudCambioCuenta(archivado, ACTOR)).aprobado()).isTrue();
        verify(usuarios, never()).bloquearAdministradoresActivos();
    }

    @Test
    @DisplayName("Con historial, la eliminación se rechaza y explica qué registros hay; el archivo no mira el historial")
    void historial() {
        Usuario docente = cuenta(5L, Rol.DOCENTE);
        when(registros.de(5L)).thenReturn(List.of(new RegistroHistorico("Casos de estudio (incluida la papelera)", 2),
                new RegistroHistorico("Sesiones programadas", 1)));

        ResultadoValidacion eliminacion = cadenas.eliminacion().validar(new SolicitudCambioCuenta(docente, ACTOR));
        assertThat(eliminacion.aprobado()).isFalse();
        assertThat(eliminacion.mensaje()).contains("Casos de estudio (incluida la papelera) (2)", "Sesiones programadas (1)",
                MensajesAdministracion.SUGERENCIA_ARCHIVAR);

        assertThat(cadenas.archivo().validar(new SolicitudCambioCuenta(docente, ACTOR)).aprobado()).isTrue();
        verify(registros, never()).de(6L);
    }

    @Test
    @DisplayName("Sin historial, la cuenta de un docente o estudiante se puede eliminar")
    void sinHistorial() {
        when(registros.de(7L)).thenReturn(List.of());
        assertThat(cadenas.eliminacion().validar(new SolicitudCambioCuenta(cuenta(7L, Rol.ESTUDIANTE), ACTOR)).aprobado()).isTrue();
    }
}
