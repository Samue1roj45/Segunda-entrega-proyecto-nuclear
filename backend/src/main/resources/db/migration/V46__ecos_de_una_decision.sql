-- Ecos de una decisión (contrato ecos-soma/1). Ver docs/ecos-de-una-decision/03-contrato.md.
-- Catálogo: una fila por versión del caso, cargada desde el banco de preguntas del juego (Caso1.json).
-- Operación: sesión de juego (código de un solo uso), resultado recibido y su recálculo.
-- Los campos JSON del contrato se guardan como TEXT para que las pruebas (H2) y MySQL usen la misma migración.

CREATE TABLE ecos_caso (
    caso_version CHAR(32) NOT NULL,                  -- MD5 de Caso1.json
    caso_id VARCHAR(32) NOT NULL,                    -- 'Caso1'
    nombre VARCHAR(255) NOT NULL,
    credito_parcial BOOLEAN NOT NULL,
    cargado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (caso_version)
);

CREATE TABLE ecos_criterio (
    caso_version CHAR(32) NOT NULL,
    criterio_id VARCHAR(8) NOT NULL,                 -- 'C1' ... 'C8'
    nombre VARCHAR(255) NOT NULL,
    PRIMARY KEY (caso_version, criterio_id),
    CONSTRAINT fk_ecos_criterio_caso FOREIGN KEY (caso_version) REFERENCES ecos_caso (caso_version)
);

CREATE TABLE ecos_pregunta (
    caso_version CHAR(32) NOT NULL,
    pregunta_id VARCHAR(16) NOT NULL,                -- 'E1_Q1' ... 'N8'
    orden INT NOT NULL,
    escenario VARCHAR(16) NOT NULL,
    nucleo BOOLEAN NOT NULL,
    puntos DECIMAL(6, 2) NOT NULL,
    enunciado TEXT NOT NULL,
    PRIMARY KEY (caso_version, pregunta_id),
    CONSTRAINT fk_ecos_pregunta_caso FOREIGN KEY (caso_version) REFERENCES ecos_caso (caso_version)
);

CREATE TABLE ecos_opcion (
    caso_version CHAR(32) NOT NULL,
    pregunta_id VARCHAR(16) NOT NULL,
    opcion_id VARCHAR(8) NOT NULL,                   -- 'A', 'P45', 'Alto'
    grado VARCHAR(10) NOT NULL,
    credito DECIMAL(4, 2) NOT NULL,
    texto TEXT NOT NULL,
    retroalimentacion TEXT NULL,
    PRIMARY KEY (caso_version, pregunta_id, opcion_id),
    CONSTRAINT ck_ecos_opcion_grado CHECK (grado IN ('Correct', 'Partial', 'Incorrect')),
    CONSTRAINT fk_ecos_opcion_pregunta FOREIGN KEY (caso_version, pregunta_id)
        REFERENCES ecos_pregunta (caso_version, pregunta_id)
);

CREATE TABLE ecos_pregunta_criterio (
    caso_version CHAR(32) NOT NULL,
    pregunta_id VARCHAR(16) NOT NULL,
    criterio_id VARCHAR(8) NOT NULL,
    PRIMARY KEY (caso_version, pregunta_id, criterio_id),
    CONSTRAINT fk_ecos_pc_pregunta FOREIGN KEY (caso_version, pregunta_id)
        REFERENCES ecos_pregunta (caso_version, pregunta_id),
    CONSTRAINT fk_ecos_pc_criterio FOREIGN KEY (caso_version, criterio_id)
        REFERENCES ecos_criterio (caso_version, criterio_id)
);

CREATE TABLE ecos_sesion (
    sesion_id CHAR(36) NOT NULL,
    codigo CHAR(9) NOT NULL,                         -- 'K7Q2-9XMP'
    estudiante_id VARCHAR(64) NOT NULL,              -- id del usuario de SOMA, como texto
    caso_version CHAR(32) NOT NULL,
    intento INT NOT NULL,
    estado VARCHAR(10) NOT NULL,
    creada_en DATETIME(6) NOT NULL,
    canjeada_en DATETIME(6) NULL,
    vence_en DATETIME(6) NOT NULL,
    ultimo_punto VARCHAR(16) NULL,
    ultimo_acto TINYINT NULL,
    PRIMARY KEY (sesion_id),
    UNIQUE KEY uk_ecos_sesion_codigo (codigo),
    KEY idx_ecos_sesion_estudiante (estudiante_id, estado),
    CONSTRAINT ck_ecos_sesion_estado CHECK (estado IN ('CREADA', 'CANJEADA', 'CERRADA', 'VENCIDA')),
    CONSTRAINT fk_ecos_sesion_caso FOREIGN KEY (caso_version) REFERENCES ecos_caso (caso_version)
);

CREATE TABLE ecos_resultado (
    envio_id CHAR(36) NOT NULL,                      -- llave de idempotencia
    sesion_id CHAR(36) NOT NULL,
    inicio DATETIME(6) NOT NULL,
    fin DATETIME(6) NOT NULL,
    segundos DECIMAL(9, 2) NOT NULL,
    protagonista VARCHAR(10) NOT NULL,
    desenlace VARCHAR(16) NOT NULL,
    puntaje DECIMAL(6, 2) NOT NULL,
    nota DECIMAL(2, 1) NOT NULL,                     -- la que calcula SOMA
    nivel VARCHAR(10) NOT NULL,
    nota_juego DECIMAL(2, 1) NULL,                   -- la que dijo el juego, solo para comparar
    difiere BOOLEAN NOT NULL DEFAULT FALSE,
    indicadores TEXT NULL,
    json_original TEXT NOT NULL,
    recibido_en DATETIME(6) NOT NULL,
    PRIMARY KEY (envio_id),
    UNIQUE KEY uk_ecos_resultado_sesion (sesion_id),
    CONSTRAINT ck_ecos_resultado_protagonista CHECK (protagonista IN ('Andrea', 'Mateo')),
    CONSTRAINT ck_ecos_resultado_desenlace CHECK (desenlace IN ('FIN_SOSTENIDA', 'FIN_FRAGIL', 'FIN_ROTA')),
    CONSTRAINT ck_ecos_resultado_nivel CHECK (nivel IN ('Superior', 'Alto', 'Básico', 'Bajo')),
    CONSTRAINT fk_ecos_resultado_sesion FOREIGN KEY (sesion_id) REFERENCES ecos_sesion (sesion_id)
);

CREATE TABLE ecos_respuesta (
    envio_id CHAR(36) NOT NULL,
    pregunta_id VARCHAR(16) NOT NULL,
    caso_version CHAR(32) NOT NULL,
    opcion_id VARCHAR(8) NULL,                       -- NULL = sin responder
    obtenido DECIMAL(6, 2) NOT NULL,
    posible DECIMAL(6, 2) NOT NULL,
    PRIMARY KEY (envio_id, pregunta_id),
    CONSTRAINT fk_ecos_respuesta_resultado FOREIGN KEY (envio_id) REFERENCES ecos_resultado (envio_id),
    CONSTRAINT fk_ecos_respuesta_pregunta FOREIGN KEY (caso_version, pregunta_id)
        REFERENCES ecos_pregunta (caso_version, pregunta_id),
    CONSTRAINT fk_ecos_respuesta_opcion FOREIGN KEY (caso_version, pregunta_id, opcion_id)
        REFERENCES ecos_opcion (caso_version, pregunta_id, opcion_id)
);

CREATE TABLE ecos_resultado_criterio (
    envio_id CHAR(36) NOT NULL,
    criterio_id VARCHAR(8) NOT NULL,
    caso_version CHAR(32) NOT NULL,
    obtenido DECIMAL(6, 2) NOT NULL,
    posible DECIMAL(6, 2) NOT NULL,
    porcentaje DECIMAL(5, 2) NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    PRIMARY KEY (envio_id, criterio_id),
    CONSTRAINT ck_ecos_rc_tipo CHECK (tipo IN ('FORTALEZA', 'MEJORA', 'NINGUNO')),
    CONSTRAINT fk_ecos_rc_resultado FOREIGN KEY (envio_id) REFERENCES ecos_resultado (envio_id),
    CONSTRAINT fk_ecos_rc_criterio FOREIGN KEY (caso_version, criterio_id)
        REFERENCES ecos_criterio (caso_version, criterio_id)
);
