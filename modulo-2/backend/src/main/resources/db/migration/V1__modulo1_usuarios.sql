-- Módulo 1 · Usuarios, autenticación y permisos (RF-01, RF-02, RF-03)
-- Motor: MySQL 8. El acceso a datos pasa por repositorios JPA para poder migrar a Oracle.

CREATE TABLE usuario (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    correo_institucional VARCHAR(150) NOT NULL,
    nombre_completo      VARCHAR(150) NOT NULL,
    rol                  VARCHAR(20)  NOT NULL,
    activo               BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_correo UNIQUE (correo_institucional),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ESTUDIANTE', 'DOCENTE', 'ADMINISTRADOR'))
);

-- Permisos asignados por el administrador a cada docente (RF-03).
-- Los permisos propios de Estudiante y Administrador se derivan del rol (enum Rol), no se guardan aquí.
CREATE TABLE usuario_permiso (
    usuario_id BIGINT      NOT NULL,
    permiso    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_usuario_permiso PRIMARY KEY (usuario_id, permiso),
    CONSTRAINT fk_usuario_permiso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    -- Debe coincidir con los valores de Permiso.asignableADocente() == true
    CONSTRAINT ck_usuario_permiso_valor CHECK (permiso IN (
        'GESTIONAR_CASOS', 'PROGRAMAR_SESIONES', 'CONSULTAR_PARTICIPACIONES',
        'DESCARGAR_REPORTES', 'REGISTRAR_RETROALIMENTACION', 'AUTORIZAR_REPETICION'))
);
