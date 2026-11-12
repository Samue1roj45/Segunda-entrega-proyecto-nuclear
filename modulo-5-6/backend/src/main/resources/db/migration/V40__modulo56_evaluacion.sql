-- Módulos 5 y 6 · Participaciones, resultados, notas oficiales, notificaciones y retroalimentación
-- (RF-12 a RF-16, RF-21, RF-24, RF-26, RF-27). Las versiones V40 a V49 quedan reservadas a estos módulos.
-- Los nombres de tablas y columnas siguen las entidades JPA. Las llaves foráneas se quedan dentro del
-- módulo: practica_id, autorizacion_id y estudiante_id se leen del módulo 3 y del módulo 1 por el puerto
-- CatalogoAcademico, así el módulo se prueba solo con un catálogo en memoria.

CREATE TABLE participacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    practica_id BIGINT NOT NULL,
    estudiante_id BIGINT NOT NULL,
    autorizacion_id BIGINT NOT NULL,
    numero_intento INT NOT NULL,
    inicio DATETIME(6) NULL,
    fin DATETIME(6) NULL,
    tiempo_usado_seg INT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_participacion_autorizacion_intento (autorizacion_id, numero_intento),
    KEY idx_participacion_practica (practica_id)
);

CREATE TABLE respuesta_seleccionada (
    id BIGINT NOT NULL AUTO_INCREMENT,
    participacion_id BIGINT NOT NULL,
    pregunta_id BIGINT NOT NULL,
    respuesta_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_respuesta_participacion_pregunta (participacion_id, pregunta_id),
    CONSTRAINT fk_respuesta_seleccionada_participacion
        FOREIGN KEY (participacion_id) REFERENCES participacion (id)
);

CREATE TABLE resultado (
    id BIGINT NOT NULL AUTO_INCREMENT,
    participacion_id BIGINT NOT NULL,
    correctas INT NOT NULL,
    incorrectas INT NOT NULL,
    no_respondidas INT NOT NULL,
    peso_obtenido INT NOT NULL,
    peso_total INT NOT NULL,
    escala_maxima INT NOT NULL,
    nota_final DECIMAL(6, 2) NOT NULL,
    fecha_calculo DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_resultado_participacion (participacion_id),
    CONSTRAINT fk_resultado_participacion
        FOREIGN KEY (participacion_id) REFERENCES participacion (id)
);

CREATE TABLE nota_oficial (
    id BIGINT NOT NULL AUTO_INCREMENT,
    practica_id BIGINT NOT NULL,
    estudiante_id BIGINT NOT NULL,
    participacion_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    fecha_seleccion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_nota_oficial_practica_estudiante (practica_id, estudiante_id),
    CONSTRAINT fk_nota_oficial_participacion
        FOREIGN KEY (participacion_id) REFERENCES participacion (id)
);

CREATE TABLE notificacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo VARCHAR(40) NOT NULL,
    clave_idempotencia VARCHAR(180) NOT NULL,
    practica_id BIGINT NULL,
    estudiante_id BIGINT NOT NULL,
    participacion_id BIGINT NULL,
    destinatario_correo VARCHAR(200) NULL,
    asunto VARCHAR(200) NOT NULL,
    cuerpo VARCHAR(4000) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    error_mensaje VARCHAR(500) NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_envio DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notificacion_clave (clave_idempotencia),
    KEY idx_notificacion_estudiante (estudiante_id),
    KEY idx_notificacion_practica (practica_id)
);

CREATE TABLE retroalimentacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    participacion_id BIGINT NOT NULL,
    estudiante_id BIGINT NOT NULL,
    practica_id BIGINT NOT NULL,
    docente_id BIGINT NOT NULL,
    texto VARCHAR(4000) NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_retroalimentacion_participacion (participacion_id),
    CONSTRAINT fk_retroalimentacion_participacion
        FOREIGN KEY (participacion_id) REFERENCES participacion (id)
);
