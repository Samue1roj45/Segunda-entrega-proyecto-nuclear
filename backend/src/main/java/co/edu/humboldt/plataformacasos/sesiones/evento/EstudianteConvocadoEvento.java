package co.edu.humboldt.plataformacasos.sesiones.evento;

/*
 #REQUISITO RF-04 || RF-20 (lo escucha el módulo 6 para RF-21)
 Patrón: OBSERVER (evento)
 Para qué sirve: un estudiante quedó autorizado en una sesión. Lleva el código de acceso EN CLARO
 solo en memoria, para que la notificación de convocatoria se lo envíe por correo; en la base solo
 queda su hash. Se atiende después de confirmar la transacción.
 */
public record EstudianteConvocadoEvento(Long idPractica, Long idAutorizacion, Long idEstudiante,
                                        String codigoAcceso) {

    @Override
    public String toString() {
        return "EstudianteConvocadoEvento[idPractica=" + idPractica + ", idAutorizacion=" + idAutorizacion
                + ", idEstudiante=" + idEstudiante + ", codigoAcceso=****]";
    }
}
