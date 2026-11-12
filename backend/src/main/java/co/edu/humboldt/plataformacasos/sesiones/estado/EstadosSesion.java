package co.edu.humboldt.plataformacasos.sesiones.estado;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Practica;/*
 #REQUISITO RF-19
 Patrón: STATE (fábrica)
 Para qué sirve: traduce el texto del estado guardado en la base al objeto de estado
 correspondiente. Los estados no guardan datos, así que se reutilizan (una sola instancia).
 */
public final class EstadosSesion {

    private static final EstadoSesion SIN_INICIAR = new SesionSinIniciar();
    private static final EstadoSesion EN_CURSO = new SesionEnCurso();
    private static final EstadoSesion FINALIZADO = new SesionFinalizada();

    private EstadosSesion() { }

    public static EstadoSesion de(String estadoBd) {
        return switch (estadoBd) {
            case Practica.SIN_INICIAR -> SIN_INICIAR;
            case Practica.EN_CURSO -> EN_CURSO;
            case Practica.FINALIZADO -> FINALIZADO;
            default -> throw new IllegalStateException("Estado de sesión desconocido: " + estadoBd);
        };
    }
}
