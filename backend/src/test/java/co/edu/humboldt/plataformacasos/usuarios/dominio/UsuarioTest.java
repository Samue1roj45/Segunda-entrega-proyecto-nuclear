package co.edu.humboldt.plataformacasos.usuarios.dominio;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {

    @Test
    void docenteNuevoNoTienePermisos() {
        Usuario docente = Usuario.nuevoDocente("Nuevo.Docente@Institucion.edu.co ", "Marta Ruiz");

        assertEquals("nuevo.docente@institucion.edu.co", docente.getCorreoInstitucional());
        assertTrue(docente.permisosEfectivos().isEmpty());
        assertTrue(docente.isActivo());
    }

    @Test
    void asignarPermisosReemplazaLosAnteriores() {
        Usuario docente = Usuario.nuevoDocente("d@institucion.edu.co", "Docente");
        docente.asignarPermisos(Set.of(Permiso.GESTIONAR_CASOS));
        docente.asignarPermisos(Set.of(Permiso.DESCARGAR_REPORTES));

        assertTrue(docente.tienePermiso(Permiso.DESCARGAR_REPORTES));
        assertFalse(docente.tienePermiso(Permiso.GESTIONAR_CASOS));
    }

    @Test
    void noSePuedeAsignarUnPermisoDeAdministradorAUnDocente() {
        Usuario docente = Usuario.nuevoDocente("d@institucion.edu.co", "Docente");

        assertThrows(IllegalArgumentException.class,
                () -> docente.asignarPermisos(Set.of(Permiso.ADMINISTRAR_DOCENTES)));
    }

    @Test
    void nombreObligatorio() {
        assertThrows(IllegalArgumentException.class, () -> Usuario.nuevoDocente("d@institucion.edu.co", " "));
    }
}
