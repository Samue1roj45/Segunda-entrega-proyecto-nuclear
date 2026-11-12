-- Módulos 3 y 4 · Estudiantes autorizados en una sesión (RF-04) y su código de acceso (RF-20).
-- El código nunca se guarda en claro: solo su hash BCrypt. El estudiante lo recibe por correo (RF-21)
-- y con él inicia sesión en el módulo 1 (VerificadorCodigoAutorizacion).
-- Retirar a un estudiante no borra la fila: queda RETIRADA para conservar el historial.

CREATE TABLE autorizaciones (
    id_autorizacion BIGINT       NOT NULL AUTO_INCREMENT,
    id_practica     BIGINT       NOT NULL,
    id_estudiante   BIGINT       NOT NULL,
    codigo_hash     VARCHAR(100) NOT NULL,
    estado          VARCHAR(15)  NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_retiro    DATETIME     NULL,
    CONSTRAINT pk_autorizaciones PRIMARY KEY (id_autorizacion),
    CONSTRAINT uk_autorizaciones_practica_estudiante UNIQUE (id_practica, id_estudiante),
    CONSTRAINT fk_autorizaciones_practica FOREIGN KEY (id_practica) REFERENCES practicas (id_practica) ON DELETE CASCADE,
    CONSTRAINT fk_autorizaciones_estudiante FOREIGN KEY (id_estudiante) REFERENCES usuario (id),
    CONSTRAINT ck_autorizaciones_estado CHECK (estado IN ('ACTIVA', 'RETIRADA'))
);

CREATE INDEX idx_autorizaciones_estudiante ON autorizaciones (id_estudiante);

-- RF-25: cada fila concede UN intento adicional (repetición excepcional) a una autorización.
CREATE TABLE autorizaciones_repeticion (
    id_repeticion      BIGINT       NOT NULL AUTO_INCREMENT,
    id_autorizacion    BIGINT       NOT NULL,
    id_docente         BIGINT       NOT NULL,
    motivo             VARCHAR(255) NULL,
    fecha_autorizacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_autorizaciones_repeticion PRIMARY KEY (id_repeticion),
    CONSTRAINT fk_repeticion_autorizacion FOREIGN KEY (id_autorizacion)
        REFERENCES autorizaciones (id_autorizacion) ON DELETE CASCADE,
    CONSTRAINT fk_repeticion_docente FOREIGN KEY (id_docente) REFERENCES usuario (id)
);
