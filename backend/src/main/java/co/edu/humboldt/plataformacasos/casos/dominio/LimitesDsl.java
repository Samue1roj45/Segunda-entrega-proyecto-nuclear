package co.edu.humboldt.plataformacasos.casos.dominio;

import java.math.BigDecimal;

/**
 * Límites de almacenamiento y dominios de valores del DSL-CEPS, en un solo lugar.
 * Las longitudes y precisiones corresponden a las columnas del diccionario de datos (Tabla 9).
 */
public final class LimitesDsl {

    public static final int LONGITUD_NOMBRE = 150;
    /** Caracteres que caben con seguridad en una columna TEXT de MySQL con utf8mb4 (65.535 bytes). */
    public static final int LONGITUD_TEXTO = 16_000;
    /** Colecciones hijas que Hibernate carga por lote, para evitar una consulta por cada elemento. */
    public static final int TAMANO_LOTE_CONSULTA = 50;

    public static final int LONGITUD_CODIGO_RESPUESTA = 2;
    /** OCL-11. */
    public static final BigDecimal PORCENTAJE_MINIMO = BigDecimal.ZERO;
    /** OCL-11. */
    public static final BigDecimal PORCENTAJE_MAXIMO = new BigDecimal("100");

    /** DECIMAL(5,2). */
    static final int DECIMALES_VALOR = 2;
    /** DECIMAL(5,2). */
    static final BigDecimal VALOR_ABSOLUTO_MAXIMO = new BigDecimal("999.99");

    private LimitesDsl() {
    }
}
