-- Módulo 2 · Creación y validación de casos (RF-05, RF-09).
-- Las versiones V20+ quedan reservadas a este módulo para no chocar con las de otros integrantes.
-- Identificadores BIGINT: es el tipo de usuario.id y el que usan los demás módulos del equipo
-- (el documento propone UUID; ver la discrepancia registrada en docs/modulo2/README.md).

CREATE TABLE casos_estudio (
    id_caso                BIGINT       NOT NULL AUTO_INCREMENT,
    id_docente             BIGINT       NOT NULL,
    -- En BORRADOR el caso puede guardarse incompleto; las reglas OCL se evalúan al publicar.
    nombre                 VARCHAR(150) NULL,
    desarrollo_situacional TEXT         NULL,
    contexto_historia      TEXT         NULL,
    tiempo_estimado_min    INT          NULL,
    estado                 VARCHAR(15)  NOT NULL DEFAULT 'BORRADOR',
    fecha_creacion         DATETIME     NOT NULL,
    fecha_actualizacion    DATETIME     NOT NULL,
    fecha_publicacion      DATETIME     NULL,
    fecha_archivado        DATETIME     NULL,
    CONSTRAINT pk_casos_estudio PRIMARY KEY (id_caso),
    -- Sin ON DELETE CASCADE: retirar a un docente no debe destruir sus casos.
    CONSTRAINT fk_casos_estudio_docente FOREIGN KEY (id_docente) REFERENCES usuario (id),
    CONSTRAINT ck_casos_estudio_estado CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'ARCHIVADO'))
);

CREATE INDEX idx_casos_estudio_estado ON casos_estudio (estado);
