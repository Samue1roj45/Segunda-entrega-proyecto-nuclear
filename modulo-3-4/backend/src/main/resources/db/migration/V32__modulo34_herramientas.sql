-- Módulo 4 · Herramientas profesionales de un caso (RF-28).
-- El docente que creó el caso registra las herramientas (calculadora, tabla, documento, enlace...) y el
-- estudiante las consulta y usa solo mientras su participación está EN_CURSO. El módulo 2 no las guarda
-- (su importación DSL las omite), por eso la tabla pertenece a este módulo.

CREATE TABLE herramientas_caso (
    id_herramienta BIGINT       NOT NULL AUTO_INCREMENT,
    id_caso        BIGINT       NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    tipo           VARCHAR(20)  NOT NULL,
    descripcion    VARCHAR(500) NULL,
    -- Definición que usa el frontend: JSON (tabla, calculadora) o URL (documento, enlace).
    contenido      TEXT         NULL,
    orden          INT          NOT NULL DEFAULT 1,
    CONSTRAINT pk_herramientas_caso PRIMARY KEY (id_herramienta),
    CONSTRAINT fk_herramientas_caso FOREIGN KEY (id_caso) REFERENCES casos_estudio (id_caso) ON DELETE CASCADE,
    CONSTRAINT ck_herramientas_tipo CHECK (tipo IN ('CALCULADORA', 'TABLA', 'DOCUMENTO', 'ENLACE', 'OTRA'))
);
