-- Módulo 2 · Escenarios y preguntas (RF-06, RF-07). Composición: dependen de su caso.

CREATE TABLE escenarios (
    id_escenario BIGINT       NOT NULL AUTO_INCREMENT,
    id_caso      BIGINT       NOT NULL,
    nombre       VARCHAR(150) NULL,
    contexto     TEXT         NULL,
    orden        INT          NOT NULL,
    CONSTRAINT pk_escenarios PRIMARY KEY (id_escenario),
    CONSTRAINT fk_escenarios_caso FOREIGN KEY (id_caso) REFERENCES casos_estudio (id_caso) ON DELETE CASCADE
);

CREATE TABLE preguntas (
    id_pregunta  BIGINT NOT NULL AUTO_INCREMENT,
    id_escenario BIGINT NOT NULL,
    enunciado    TEXT   NULL,
    orden        INT    NOT NULL,
    CONSTRAINT pk_preguntas PRIMARY KEY (id_pregunta),
    CONSTRAINT fk_preguntas_escenario FOREIGN KEY (id_escenario) REFERENCES escenarios (id_escenario) ON DELETE CASCADE
);
