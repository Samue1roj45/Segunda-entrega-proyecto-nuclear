package com.unihumboldt.nuclear.evaluacion.notificacion;

public record ResumenEnvio(int enviadas, int omitidasDuplicado, int errores) {

    public ResumenEnvio acumular(ResultadoEnvio resultado) {
        return switch (resultado) {
            case ENVIADA -> new ResumenEnvio(enviadas + 1, omitidasDuplicado, errores);
            case OMITIDA_DUPLICADO -> new ResumenEnvio(enviadas, omitidasDuplicado + 1, errores);
            case ERROR -> new ResumenEnvio(enviadas, omitidasDuplicado, errores + 1);
        };
    }

    public ResumenEnvio acumular(ResumenEnvio otro) {
        return new ResumenEnvio(
                enviadas + otro.enviadas(),
                omitidasDuplicado + otro.omitidasDuplicado(),
                errores + otro.errores());
    }
}
