package co.edu.humboldt.plataformacasos.casos.dominio;

/**
 * Límites de almacenamiento y cantidades del DSL-CEPS, en un solo lugar.
 * Las longitudes corresponden a las columnas del diccionario de datos (Tabla 9).
 */
public final class LimitesDsl {

    public static final int LONGITUD_NOMBRE = 150;
    /** Caracteres que caben con seguridad en una columna TEXT de MySQL con utf8mb4 (65.535 bytes). */
    public static final int LONGITUD_TEXTO = 16_000;

    private LimitesDsl() {
    }
}
