-- Módulo 2 · Opciones de respuesta (RF-07).
-- codigo no es UNIQUE por pregunta: un borrador puede tener duplicados y OCL-08 los informa al publicar.
-- porcentaje admite NULL (borrador incompleto), pero nunca un valor fuera de dominio (sección 6.4).

CREATE TABLE respuestas (
    id_respuesta BIGINT       NOT NULL AUTO_INCREMENT,
    id_pregunta  BIGINT       NOT NULL,
    codigo       VARCHAR(2)   NULL,
    texto        TEXT         NULL,
    es_correcta  BOOLEAN      NULL,
    porcentaje   DECIMAL(5,2) NULL,
    CONSTRAINT pk_respuestas PRIMARY KEY (id_respuesta),
    CONSTRAINT fk_respuestas_pregunta FOREIGN KEY (id_pregunta) REFERENCES preguntas (id_pregunta) ON DELETE CASCADE,
    CONSTRAINT ck_respuestas_porcentaje CHECK (porcentaje IS NULL OR (porcentaje >= 0 AND porcentaje <= 100))
);
