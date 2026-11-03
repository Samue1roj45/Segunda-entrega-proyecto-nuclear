package com.example.demo.state;
/*
 #REQUISITO RF-19
 Patrón: STATE (fábrica)
 Para qué sirve: traduce el texto del estado guardado en BD al objeto de estado
 correspondiente. Los estados no guardan datos, así que se reutilizan (una sola instancia).
 */
public final class EstadosSesion {

    private static final EstadoSesion SIN_INICIAR = new SesionSinIniciar();
    private static final EstadoSesion EN_CURSO = new SesionEnCurso();
    private static final EstadoSesion FINALIZADO = new SesionFinalizada();

    private EstadosSesion() { }

    public static EstadoSesion de(String estadoBd) {
        return switch (estadoBd) {
            case "SIN_INICIAR" -> SIN_INICIAR;
            case "EN_CURSO" -> EN_CURSO;
            case "FINALIZADO" -> FINALIZADO;
            default -> throw new IllegalStateException("Estado de sesión desconocido: " + estadoBd);
        };
    }
}
