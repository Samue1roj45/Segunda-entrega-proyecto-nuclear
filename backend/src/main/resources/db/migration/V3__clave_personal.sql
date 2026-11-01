-- Inicio de sesión del personal (docentes y administrador) con correo y contraseña.
-- RF-01 solo define el ingreso de estudiantes; RF-02 y RF-03 requieren que el personal
-- también se autentique. Supuesto a validar con el equipo. Solo se guarda el hash BCrypt.
ALTER TABLE usuario ADD COLUMN clave_hash VARCHAR(100) NULL;
