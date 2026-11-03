package co.edu.humboldt.plataformacasos.casos.dominio;

import co.edu.humboldt.plataformacasos.compartido.excepcion.SolicitudInvalidaException;

import java.math.BigDecimal;

/** Comprueba que un valor quepa en DECIMAL(5,2) sin redondeos silenciosos. */
final class DecimalDominio {

    private DecimalDominio() {
    }

    static void exigirPrecision(BigDecimal valor, String campo) {
        if (valor == null) {
            return;
        }
        boolean excedeDecimales = valor.stripTrailingZeros().scale() > LimitesDsl.DECIMALES_VALOR;
        if (excedeDecimales || valor.abs().compareTo(LimitesDsl.VALOR_ABSOLUTO_MAXIMO) > 0) {
            throw new SolicitudInvalidaException("El campo '" + campo + "' admite hasta "
                    + LimitesDsl.DECIMALES_VALOR + " decimales y un máximo de "
                    + LimitesDsl.VALOR_ABSOLUTO_MAXIMO + ".");
        }
    }
}
