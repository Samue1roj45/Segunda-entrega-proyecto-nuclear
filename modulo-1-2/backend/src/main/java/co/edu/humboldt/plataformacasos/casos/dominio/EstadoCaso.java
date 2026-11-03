package co.edu.humboldt.plataformacasos.casos.dominio;

/**
 * Ciclo de vida del caso (RF-09). Lo gestiona el sistema: no forma parte del documento DSL-CEPS.
 *
 * <p>Las reglas de cada estado viven en el propio enum. Con tres estados y dos transiciones,
 * una jerarquía de clases del patrón State añadiría complejidad sin resolver ningún problema.</p>
 */
public enum EstadoCaso {

    /** Se edita libremente y puede guardarse incompleto. */
    BORRADOR,
    /** Disponible para nuevas programaciones. Su contenido ya no cambia. */
    PUBLICADO,
    /** Conservado para el histórico; no admite nuevas programaciones. */
    ARCHIVADO;

    public boolean permiteEditarContenido() {
        return this == BORRADOR;
    }

    public boolean disponibleParaProgramar() {
        return this == PUBLICADO;
    }

    public boolean puedeTransitarA(EstadoCaso destino) {
        return (this == BORRADOR && destino == PUBLICADO) || (this == PUBLICADO && destino == ARCHIVADO);
    }
}
