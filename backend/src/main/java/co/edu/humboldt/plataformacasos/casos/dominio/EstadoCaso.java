package co.edu.humboldt.plataformacasos.casos.dominio;

/**
 * Ciclo de vida del caso (RF-09). Lo gestiona el sistema: no forma parte del documento DSL-CEPS.
 *
 * <p>Las reglas de cada estado viven en el propio enum. Con tres estados y tres transiciones,
 * una jerarquía de clases del patrón State añadiría complejidad sin resolver ningún problema.</p>
 *
 * <p>Transiciones: BORRADOR → PUBLICADO (publicar), PUBLICADO → ARCHIVADO (archivar) y
 * ARCHIVADO → PUBLICADO (recuperar). Archivar no es eliminar: el docente guarda el caso para volver a
 * usarlo, por eso puede recuperarlo. La papelera es otro mecanismo (borrado lógico) y no cambia el estado.</p>
 */
public enum EstadoCaso {

    /** Se edita libremente y puede guardarse incompleto. */
    BORRADOR,
    /** Disponible para nuevas programaciones. Su contenido ya no cambia. */
    PUBLICADO,
    /** Conservado para el histórico y para reutilizarlo; no admite nuevas programaciones hasta recuperarlo. */
    ARCHIVADO;

    public boolean permiteEditarContenido() {
        return this == BORRADOR;
    }

    public boolean disponibleParaProgramar() {
        return this == PUBLICADO;
    }

    public boolean puedeTransitarA(EstadoCaso destino) {
        return (this == BORRADOR && destino == PUBLICADO)
                || (this == PUBLICADO && destino == ARCHIVADO)
                || (this == ARCHIVADO && destino == PUBLICADO);
    }

    /** Publicar es la primera salida del borrador; un caso archivado vuelve a PUBLICADO con «recuperar». */
    public boolean puedePublicarse() {
        return this == BORRADOR;
    }

    public boolean puedeRecuperarse() {
        return this == ARCHIVADO;
    }
}
