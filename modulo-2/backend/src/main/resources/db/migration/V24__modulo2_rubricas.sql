-- Módulo 2 · Rúbricas y criterios de evaluación (RF-17).
-- Un caso tiene como máximo una rúbrica (id_caso único) y en BORRADOR puede no tenerla.
-- El rango admite NULL (borrador incompleto), pero nunca min >= max (sección 6.4).
-- Sin pesos, porcentajes ni fórmulas: los documentos no los definen y el cálculo es del Integrante 4.

CREATE TABLE rubricas (
    id_rubrica  BIGINT       NOT NULL AUTO_INCREMENT,
    id_caso     BIGINT       NOT NULL,
    nombre      VARCHAR(150) NULL,
    descripcion TEXT         NULL,
    CONSTRAINT pk_rubricas PRIMARY KEY (id_rubrica),
    CONSTRAINT uk_rubricas_caso UNIQUE (id_caso),
    CONSTRAINT fk_rubricas_caso FOREIGN KEY (id_caso) REFERENCES casos_estudio (id_caso) ON DELETE CASCADE
);

CREATE TABLE criterios_evaluacion (
    id_criterio BIGINT       NOT NULL AUTO_INCREMENT,
    id_rubrica  BIGINT       NOT NULL,
    criterio    VARCHAR(150) NULL,
    parametros  TEXT         NULL,
    rango_min   DECIMAL(5,2) NULL,
    rango_max   DECIMAL(5,2) NULL,
    CONSTRAINT pk_criterios_evaluacion PRIMARY KEY (id_criterio),
    CONSTRAINT fk_criterios_rubrica FOREIGN KEY (id_rubrica) REFERENCES rubricas (id_rubrica) ON DELETE CASCADE,
    CONSTRAINT ck_criterios_rango CHECK (rango_min IS NULL OR rango_max IS NULL OR rango_min < rango_max)
);
