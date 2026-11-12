-- Módulos 3 y 4 · Programación de sesiones (RF-04, RF-18, RF-19).
-- Las versiones V30 a V39 quedan reservadas a estos módulos. Una "práctica" es la sesión que un docente
-- programa sobre un caso PUBLICADO del módulo 2. Las reglas de negocio están en Java (Chain, State, Strategy);
-- la base solo garantiza la integridad (llaves foráneas, valores válidos y fechas coherentes).

CREATE TABLE practicas (
    id_practica                      BIGINT       NOT NULL AUTO_INCREMENT,
    id_caso                          BIGINT       NOT NULL,
    id_docente                       BIGINT       NOT NULL,
    espacio_academico                VARCHAR(150) NOT NULL,
    fecha_inicio                     DATETIME     NOT NULL,
    fecha_fin                        DATETIME     NOT NULL,
    tiempo_maximo_minutos            INT          NOT NULL,
    -- RF-22: porcentaje del tiempo máximo en el que aún se permite ingresar (NULL = 50 %).
    porcentaje_limite_ingreso_tardio DECIMAL(5,2) NULL,
    lugar                            VARCHAR(150) NULL,
    mensaje_personalizado            TEXT         NULL,
    url_acceso                       VARCHAR(255) NULL,
    estado_practica                  VARCHAR(15)  NOT NULL DEFAULT 'SIN_INICIAR',
    fecha_inicio_real                DATETIME     NULL,
    fecha_cierre_real                DATETIME     NULL,
    tipo_cierre                      VARCHAR(15)  NULL,
    fecha_creacion                   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_practicas PRIMARY KEY (id_practica),
    CONSTRAINT fk_practicas_caso FOREIGN KEY (id_caso) REFERENCES casos_estudio (id_caso),
    CONSTRAINT fk_practicas_docente FOREIGN KEY (id_docente) REFERENCES usuario (id),
    CONSTRAINT ck_practicas_estado CHECK (estado_practica IN ('SIN_INICIAR', 'EN_CURSO', 'FINALIZADO')),
    CONSTRAINT ck_practicas_tipo_cierre CHECK (tipo_cierre IS NULL OR tipo_cierre IN ('MANUAL', 'AUTOMATICO')),
    CONSTRAINT ck_practicas_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_practicas_tiempo CHECK (tiempo_maximo_minutos > 0),
    CONSTRAINT ck_practicas_porcentaje CHECK (porcentaje_limite_ingreso_tardio IS NULL
        OR (porcentaje_limite_ingreso_tardio > 0 AND porcentaje_limite_ingreso_tardio <= 100))
);

CREATE INDEX idx_practicas_docente ON practicas (id_docente);
CREATE INDEX idx_practicas_estado ON practicas (estado_practica);
