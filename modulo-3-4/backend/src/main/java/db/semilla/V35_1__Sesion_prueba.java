package db.semilla;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Datos FICTICIOS (perfiles "pruebas" y "test"): una sesión SIN_INICIAR de Carlos Gómez sobre el caso de
 * prueba del módulo 2, con Ana y Luis convocados. Sus códigos de acceso son los del documento PT-DCP-01
 * (RF-01) y se guardan solo como hash BCrypt. La sesión dura 30 días desde que se crea la base.
 */
public class V35_1__Sesion_prueba extends BaseJavaMigration {

    private static final Map<String, String> CODIGOS_POR_CORREO = new LinkedHashMap<>();

    static {
        CODIGOS_POR_CORREO.put("ana.perez@institucion.edu.co", "CODIGO_VALIDO_01");
        CODIGOS_POR_CORREO.put("luis.rios@institucion.edu.co", "CODIGO_VALIDO_02");
    }

    @Override
    public void migrate(Context context) throws SQLException {
        var conexion = context.getConnection();
        LocalDateTime inicio = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        long idPractica;
        try (PreparedStatement insercion = conexion.prepareStatement("""
                INSERT INTO practicas (id_caso, id_docente, espacio_academico, fecha_inicio, fecha_fin,
                                       tiempo_maximo_minutos, lugar, mensaje_personalizado, estado_practica,
                                       fecha_creacion)
                SELECT c.id_caso, c.id_docente, 'Psicología Social I', ?, ?, 60, 'Laboratorio 3',
                       'Traer el material de lectura de la semana 4.', 'SIN_INICIAR', ?
                  FROM casos_estudio c WHERE c.nombre = 'Conformidad en el grupo de trabajo'
                """, new String[] {"id_practica"})) {
            insercion.setTimestamp(1, Timestamp.valueOf(inicio));
            insercion.setTimestamp(2, Timestamp.valueOf(inicio.plusDays(30)));
            insercion.setTimestamp(3, Timestamp.valueOf(inicio));
            insercion.executeUpdate();
            try (ResultSet llave = insercion.getGeneratedKeys()) {
                llave.next();
                idPractica = llave.getLong(1);
            }
        }

        PasswordEncoder codificador = new BCryptPasswordEncoder();
        try (PreparedStatement insercion = conexion.prepareStatement("""
                INSERT INTO autorizaciones (id_practica, id_estudiante, codigo_hash, estado, fecha_creacion)
                SELECT ?, id, ?, 'ACTIVA', ? FROM usuario WHERE correo_institucional = ?
                """)) {
            for (Map.Entry<String, String> codigo : CODIGOS_POR_CORREO.entrySet()) {
                insercion.setLong(1, idPractica);
                insercion.setString(2, codificador.encode(codigo.getValue()));
                insercion.setTimestamp(3, Timestamp.valueOf(inicio));
                insercion.setString(4, codigo.getKey());
                insercion.addBatch();
            }
            insercion.executeBatch();
        }
    }
}
