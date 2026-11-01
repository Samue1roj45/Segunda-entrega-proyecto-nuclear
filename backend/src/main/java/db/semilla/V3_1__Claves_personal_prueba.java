package db.semilla;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/** Datos FICTICIOS (perfil "pruebas"/"test"): contraseñas del personal de prueba, cifradas con BCrypt. */
public class V3_1__Claves_personal_prueba extends BaseJavaMigration {

    private static final Map<String, String> CLAVES_POR_CORREO = Map.of(
            "admin.plataforma@institucion.edu.co", "Clave-Admin-01",
            "carlos.gomez@institucion.edu.co", "Clave-Docente-01",
            "laura.mejia@institucion.edu.co", "Clave-Docente-02");

    @Override
    public void migrate(Context context) throws SQLException {
        PasswordEncoder codificador = new BCryptPasswordEncoder();
        String sql = "UPDATE usuario SET clave_hash = ? WHERE correo_institucional = ?";
        try (PreparedStatement actualizacion = context.getConnection().prepareStatement(sql)) {
            for (Map.Entry<String, String> clave : CLAVES_POR_CORREO.entrySet()) {
                actualizacion.setString(1, codificador.encode(clave.getValue()));
                actualizacion.setString(2, clave.getKey());
                actualizacion.addBatch();
            }
            actualizacion.executeBatch();
        }
    }
}
