package co.edu.humboldt.plataformacasos.sesiones.dominio;

import java.time.LocalDateTime;

/*
 #REQUISITO RF-04 || RF-20
 Para qué sirve: un estudiante autorizado en una sesión. El código de acceso solo existe como hash.
 */
public record Autorizacion(Long id, Long idPractica, Long idEstudiante, String codigoHash, String estado,
                           LocalDateTime fechaCreacion) {

    public static final String ACTIVA = "ACTIVA";
    public static final String RETIRADA = "RETIRADA";

    public boolean activa() {
        return ACTIVA.equals(estado);
    }
}
