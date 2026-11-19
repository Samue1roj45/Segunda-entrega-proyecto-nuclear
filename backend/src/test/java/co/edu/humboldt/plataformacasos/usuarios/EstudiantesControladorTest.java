package co.edu.humboldt.plataformacasos.usuarios;

import co.edu.humboldt.plataformacasos.autorizacion.ServicioAutorizacion;
import co.edu.humboldt.plataformacasos.compartido.excepcion.AccesoDenegadoException;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Rol;
import co.edu.humboldt.plataformacasos.usuarios.dominio.Usuario;
import co.edu.humboldt.plataformacasos.usuarios.repositorio.UsuarioRepositorio;
import co.edu.humboldt.plataformacasos.usuarios.web.EstudiantesControlador;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class EstudiantesControladorTest {

    @Test
    void listaSoloEstudiantesActivosYExigeElPermiso() {
        UsuarioRepositorio repo = mock(UsuarioRepositorio.class);
        ServicioAutorizacion aut = mock(ServicioAutorizacion.class);
        Usuario activo = mock(Usuario.class);
        when(activo.isActivo()).thenReturn(true);
        when(activo.getId()).thenReturn(7L);
        when(activo.getNombreCompleto()).thenReturn("Ana Pérez");
        when(activo.getCorreoInstitucional()).thenReturn("ana@x.co");
        Usuario inactivo = mock(Usuario.class);
        when(inactivo.isActivo()).thenReturn(false);
        when(repo.findByRolOrderByNombreCompletoAsc(Rol.ESTUDIANTE)).thenReturn(List.of(activo, inactivo));

        var lista = new EstudiantesControlador(repo, aut).listar();

        verify(aut).exigirPermiso(Permiso.PROGRAMAR_SESIONES);
        assertEquals(1, lista.size());
        assertEquals("Ana Pérez", lista.get(0).nombreCompleto());
    }

    @Test
    void sinPermisoNoListaNada() {
        UsuarioRepositorio repo = mock(UsuarioRepositorio.class);
        ServicioAutorizacion aut = mock(ServicioAutorizacion.class);
        doThrow(new AccesoDenegadoException("no")).when(aut).exigirPermiso(any());
        assertThrows(AccesoDenegadoException.class, () -> new EstudiantesControlador(repo, aut).listar());
    }
}
