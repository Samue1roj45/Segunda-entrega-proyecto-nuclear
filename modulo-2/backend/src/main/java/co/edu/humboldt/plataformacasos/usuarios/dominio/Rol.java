package co.edu.humboldt.plataformacasos.usuarios.dominio;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Roles del sistema. Cada rol trae permisos base fijos; a los docentes el
 * administrador les asigna además permisos individuales (RF-03).
 */
public enum Rol {

    ESTUDIANTE(EnumSet.of(Permiso.PARTICIPAR_EN_CASOS, Permiso.CONSULTAR_RESULTADOS_PROPIOS)),
    DOCENTE(EnumSet.noneOf(Permiso.class)),
    ADMINISTRADOR(EnumSet.of(Permiso.ADMINISTRAR_DOCENTES));

    private final Set<Permiso> permisosBase;

    Rol(Set<Permiso> permisosBase) {
        this.permisosBase = Collections.unmodifiableSet(permisosBase);
    }

    public Set<Permiso> permisosBase() {
        return permisosBase;
    }
}
