package db.semilla;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Datos FICTICIOS (perfil "pruebas"/"test"): códigos de autorización de los estudiantes de prueba
 * del documento PT-DCP-01. Se cifran con BCrypt al migrar, así ningún hash queda escrito a mano.
 */
public class V2_1__Codigos_autorizacion_prueba extends BaseJavaMigration {

    private static final Map<String, String> CODIGOS_POR_CORREO = Map.of(
            "ana.perez@institucion.edu.co", "CODIGO_VALIDO_01",
            "luis.rios@institucion.edu.co", "CODIGO_VALIDO_02");

    @Override
    public void migrate(Context context) throws SQLException {
        PasswordEncoder codificador = new BCryptPasswordEncoder();
        String sql = "INSERT INTO codigo_autorizacion_temporal (estudiante_id, codigo_hash) "
                + "SELECT id, ? FROM usuario WHERE correo_institucional = ?";
        try (PreparedStatement insercion = context.getConnection().prepareStatement(sql)) {
            for (Map.Entry<String, String> codigo : CODIGOS_POR_CORREO.entrySet()) {
                insercion.setString(1, codificador.encode(codigo.getValue()));
                insercion.setString(2, codigo.getKey());
                insercion.addBatch();
            }
            insercion.executeBatch();
        }
    }
}
