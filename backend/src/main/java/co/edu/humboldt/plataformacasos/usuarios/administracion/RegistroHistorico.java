package co.edu.humboldt.plataformacasos.usuarios.administracion;

/** Un tipo de registro que depende de una cuenta (por ejemplo, «Participaciones», 3). */
public record RegistroHistorico(String descripcion, long cantidad) {
}
