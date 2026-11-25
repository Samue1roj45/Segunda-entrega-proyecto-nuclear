package co.edu.humboldt.plataformacasos.integracion;

import co.edu.humboldt.plataformacasos.usuarios.administracion.RegistroHistorico;
import co.edu.humboldt.plataformacasos.usuarios.administracion.RegistrosHistoricosUsuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador del puerto {@link RegistrosHistoricosUsuario}: cuenta, tabla por tabla, lo que depende de una
 * cuenta en los módulos 2 a 6. Se revisan también las tablas sin llave foránea hacia {@code usuario}
 * (participación, nota oficial, notificación, retroalimentación), que la base no protegería.
 *
 * <p>No se cuentan los permisos, los avisos de la campana ni las preferencias (la base los borra en cascada
 * con la cuenta) ni la auditoría (guarda el correo del actor como texto y sigue siendo legible).</p>
 */
@Component
public class RegistrosHistoricosUsuarioJdbc implements RegistrosHistoricosUsuario {

    private record Consulta(String descripcion, String sql, int parametros) {
    }

    private static final List<Consulta> CONSULTAS = List.of(
            new Consulta("Casos de estudio (incluida la papelera)", "SELECT COUNT(*) FROM casos_estudio WHERE id_docente = ?", 1),
            new Consulta("Sesiones programadas", "SELECT COUNT(*) FROM practicas WHERE id_docente = ?", 1),
            new Consulta("Convocatorias a sesiones", "SELECT COUNT(*) FROM autorizaciones WHERE id_estudiante = ?", 1),
            new Consulta("Repeticiones autorizadas", "SELECT COUNT(*) FROM autorizaciones_repeticion WHERE id_docente = ?", 1),
            new Consulta("Participaciones", "SELECT COUNT(*) FROM participacion WHERE estudiante_id = ?", 1),
            new Consulta("Notas oficiales", "SELECT COUNT(*) FROM nota_oficial WHERE estudiante_id = ? OR docente_id = ?", 2),
            new Consulta("Retroalimentaciones", "SELECT COUNT(*) FROM retroalimentacion WHERE estudiante_id = ? OR docente_id = ?", 2),
            new Consulta("Notificaciones enviadas", "SELECT COUNT(*) FROM notificacion WHERE estudiante_id = ?", 1));

    private final JdbcTemplate jdbc;

    public RegistrosHistoricosUsuarioJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RegistroHistorico> de(Long idUsuario) {
        List<RegistroHistorico> encontrados = new ArrayList<>();
        for (Consulta consulta : CONSULTAS) {
            Object[] parametros = consulta.parametros() == 2 ? new Object[]{idUsuario, idUsuario} : new Object[]{idUsuario};
            Long cantidad = jdbc.queryForObject(consulta.sql(), Long.class, parametros);
            if (cantidad != null && cantidad > 0) {
                encontrados.add(new RegistroHistorico(consulta.descripcion(), cantidad));
            }
        }
        return encontrados;
    }
}
