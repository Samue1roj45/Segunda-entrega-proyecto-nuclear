package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.RecursoNoEncontradoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas del servicio real de RF-03 (sin el proxy de acceso). */
class ServicioAdministracionDocentesImplTest {

    private final UsuarioRepositorio repositorio = mock(UsuarioRepositorio.class);
    private final ServicioAdministracionDocentesImpl servicio = new ServicioAdministracionDocentesImpl(repositorio);

    @Test
    void creaDocenteActivoSinPermisosConCorreoNormalizado() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        DocenteDTO creado = servicio.crearDocente(new CrearDocenteRequest("Marta Ruiz", " Nuevo.Docente@Institucion.edu.co "));

        assertThat(creado.correoInstitucional()).isEqualTo("nuevo.docente@institucion.edu.co");
        assertThat(creado.activo()).isTrue();
        assertThat(creado.permisos()).isEmpty();
    }

    @Test
    void rechazaNombreVacio() {
        assertThatThrownBy(() -> servicio.crearDocente(new CrearDocenteRequest(" ", "a@institucion.edu.co")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(MensajesAdministracion.NOMBRE_OBLIGATORIO);
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaCorreoConFormatoInvalido() {
        assertThatThrownBy(() -> servicio.crearDocente(new CrearDocenteRequest("Marta", "sin-arroba")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(MensajesAdministracion.CORREO_INVALIDO);
    }

    @Test
    void rechazaCorreoDuplicado() {
        when(repositorio.existsByCorreoInstitucional("carlos.gomez@institucion.edu.co")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crearDocente(new CrearDocenteRequest("Carlos", "carlos.gomez@institucion.edu.co")))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(MensajesAdministracion.CORREO_DUPLICADO);
        verify(repositorio, never()).save(any());
    }

    @Test
    void asignaPermisosAUnDocente() {
        Usuario docente = Usuario.nuevoDocente("d@institucion.edu.co", "Docente");
        when(repositorio.findById(7L)).thenReturn(Optional.of(docente));

        DocenteDTO actualizado = servicio.asignarPermisos(7L, Set.of(Permiso.DESCARGAR_REPORTES));

        assertThat(actualizado.permisos()).containsExactly(Permiso.DESCARGAR_REPORTES);
    }

    @Test
    void rechazaPermisoNoAsignableADocentes() {
        when(repositorio.findById(7L)).thenReturn(Optional.of(Usuario.nuevoDocente("d@institucion.edu.co", "D")));

        assertThatThrownBy(() -> servicio.asignarPermisos(7L, Set.of(Permiso.ADMINISTRAR_DOCENTES)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void noAsignaPermisosAQuienNoEsDocente() {
        Usuario estudiante = mock(Usuario.class);
        when(estudiante.getRol()).thenReturn(Rol.ESTUDIANTE);
        when(repositorio.findById(4L)).thenReturn(Optional.of(estudiante));

        assertThatThrownBy(() -> servicio.asignarPermisos(4L, Set.of()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void exigeLaListaDePermisos() {
        assertThatThrownBy(() -> servicio.asignarPermisos(7L, null))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(MensajesAdministracion.PERMISOS_OBLIGATORIOS);
    }
}
