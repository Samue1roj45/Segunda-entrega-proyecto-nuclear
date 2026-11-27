package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.compartido.excepcion.ConflictoException;
import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas del servicio real de registro de estudiantes (sin el proxy de acceso). */
class ServicioAdministracionEstudiantesImplTest {

    private final UsuarioRepositorio repositorio = mock(UsuarioRepositorio.class);
    private final ServicioAdministracionEstudiantesImpl servicio =
            new ServicioAdministracionEstudiantesImpl(repositorio, " Institucion.edu.co ");

    @Test
    void creaEstudianteActivoConCorreoNormalizado() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteDTO creado = servicio.crearEstudiante(
                new CrearEstudianteRequest(" Sofía Restrepo ", " Sofia.Restrepo@Institucion.edu.co "));

        assertThat(creado.correoInstitucional()).isEqualTo("sofia.restrepo@institucion.edu.co");
        assertThat(creado.nombreCompleto()).isEqualTo("Sofía Restrepo");
        assertThat(creado.activo()).isTrue();
        verify(repositorio).save(any(Usuario.class));
    }

    @Test
    void elUsuarioCreadoTieneRolEstudiante() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            assertThat(u.getRol()).isEqualTo(Rol.ESTUDIANTE);
            return u;
        });

        servicio.crearEstudiante(new CrearEstudianteRequest("Pedro", "pedro@institucion.edu.co"));
    }

    @Test
    void rechazaNombreVacio() {
        assertThatThrownBy(() -> servicio.crearEstudiante(new CrearEstudianteRequest(" ", "a@institucion.edu.co")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(MensajesAdministracion.NOMBRE_OBLIGATORIO);
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaCorreoVacio() {
        assertThatThrownBy(() -> servicio.crearEstudiante(new CrearEstudianteRequest("Pedro", "")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(MensajesAdministracion.CORREO_OBLIGATORIO);
    }

    @Test
    void rechazaCorreoConFormatoInvalido() {
        assertThatThrownBy(() -> servicio.crearEstudiante(new CrearEstudianteRequest("Pedro", "sin-arroba")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage(MensajesAdministracion.CORREO_INVALIDO);
    }

    @Test
    void rechazaCorreoDeOtroDominioPorqueNoPodriaIngresar() {
        assertThatThrownBy(() -> servicio.crearEstudiante(new CrearEstudianteRequest("Pedro", "pedro@gmail.com")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("@institucion.edu.co");
        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazaSubdominioQueSoloTerminaParecido() {
        assertThatThrownBy(() -> servicio.crearEstudiante(new CrearEstudianteRequest("Pedro", "pedro@falsa-institucion.edu.co")))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void rechazaCorreoDuplicado() {
        when(repositorio.existsByCorreoInstitucional("ana.perez@institucion.edu.co")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crearEstudiante(new CrearEstudianteRequest("Ana", "ana.perez@institucion.edu.co")))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(MensajesAdministracion.CORREO_DUPLICADO);
        verify(repositorio, never()).save(any());
    }

    @Test
    void listaSoloEstudiantes() {
        Usuario ana = Usuario.nuevoEstudiante("ana.perez@institucion.edu.co", "Ana Pérez");
        when(repositorio.findByRolOrderByNombreCompletoAsc(Rol.ESTUDIANTE)).thenReturn(List.of(ana));

        assertThat(servicio.listarEstudiantes()).extracting(EstudianteDTO::correoInstitucional)
                .containsExactly("ana.perez@institucion.edu.co");
    }
}
