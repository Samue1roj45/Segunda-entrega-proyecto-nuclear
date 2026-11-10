package co.edu.humboldt.plataformacasos.usuarios.administracion;

import co.edu.humboldt.plataformacasos.usuarios.dominio.Permiso;

import java.util.Set;

/** Cuerpo de PUT /api/admin/docentes/{id}/permisos: reemplaza el conjunto de permisos del docente. */
public record AsignarPermisosRequest(Set<Permiso> permisos) {
}
