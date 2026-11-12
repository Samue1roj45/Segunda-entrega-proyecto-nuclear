package co.edu.humboldt.plataformacasos.sesiones.dto;

import co.edu.humboldt.plataformacasos.sesiones.dominio.Herramienta;
import co.edu.humboldt.plataformacasos.sesiones.dominio.TipoHerramienta;

/* #REQUISITO RF-28 · Herramienta profesional tal como la recibe el frontend. */
public record HerramientaDto(Long idHerramienta, String nombre, TipoHerramienta tipo, String descripcion,
                             String contenido, int orden) {

    public static HerramientaDto de(Herramienta h) {
        return new HerramientaDto(h.id(), h.nombre(), h.tipo(), h.descripcion(), h.contenido(), h.orden());
    }
}
