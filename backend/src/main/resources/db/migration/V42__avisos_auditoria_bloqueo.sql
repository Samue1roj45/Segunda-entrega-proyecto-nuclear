-- Avisos dentro de la plataforma (campana), registro de auditoría y control de intentos de ingreso.

CREATE TABLE aviso (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT       NOT NULL,
    tipo       VARCHAR(40)  NOT NULL,
    titulo     VARCHAR(150) NOT NULL,
    mensaje    VARCHAR(500) NOT NULL,
    leido      BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_aviso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);
CREATE INDEX idx_aviso_usuario ON aviso (usuario_id, leido, id);

CREATE TABLE auditoria (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    fecha        DATETIME(6)  NOT NULL,
    actor_id     BIGINT       NULL,
    actor_correo VARCHAR(150) NULL,
    accion       VARCHAR(60)  NOT NULL,
    detalle      VARCHAR(500) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX idx_auditoria_fecha ON auditoria (fecha);

CREATE TABLE bloqueo_acceso (
    correo          VARCHAR(150) NOT NULL,
    intentos        INT          NOT NULL DEFAULT 0,
    bloqueado_hasta DATETIME(6)  NULL,
    ultimo_intento  DATETIME(6)  NOT NULL,
    PRIMARY KEY (correo)
);
