package co.edu.humboldt.plataformacasos.usuarios.dominio;

/**
 * Catálogo de permisos (RF-02, RF-03). Cada permiso habilita funciones de un
 * requisito existente; no se agregan permisos sin un requisito que los respalde.
 */
public enum Permiso {

    // Propios del rol Estudiante (no asignables a docentes)
    PARTICIPAR_EN_CASOS("Participar en casos y usar sus herramientas (RF-10, RF-11, RF-28)", false),
    CONSULTAR_RESULTADOS_PROPIOS("Consultar el resultado final propio (RF-14)", false),

    // Asignables por el administrador a cada docente (RF-03)
    GESTIONAR_CASOS("Crear, editar y publicar casos y rúbricas (RF-05 a RF-09, RF-17)", true),
    PROGRAMAR_SESIONES("Asociar estudiantes y programar convocatorias (RF-04, RF-18, RF-20, RF-21)", true),
    CONSULTAR_PARTICIPACIONES("Consultar participaciones (RF-15)", true),
    DESCARGAR_REPORTES("Descargar reportes (RF-16)", true),
    REGISTRAR_RETROALIMENTACION("Registrar valoración docente (RF-26)", true),
    AUTORIZAR_REPETICION("Autorizar repetición excepcional (RF-25)", true),

    // Propio del rol Administrador (no asignable a docentes)
    ADMINISTRAR_DOCENTES("Crear docentes y asignar permisos (RF-03)", false);

    private final String descripcion;
    private final boolean asignableADocente;

    Permiso(String descripcion, boolean asignableADocente) {
        this.descripcion = descripcion;
        this.asignableADocente = asignableADocente;
    }

    public String descripcion() {
        return descripcion;
    }

    public boolean asignableADocente() {
        return asignableADocente;
    }
}
