-- Invitación de estudiantes por correo, perfil del estudiante, vínculo docente-estudiante y ampliaciones de Ecos.
-- Retirar a un estudiante de un docente solo borra el vínculo: la cuenta del estudiante no se toca.

-- Permiso del docente para invitar y administrar a sus estudiantes (y ver sus resultados de Ecos).
ALTER TABLE usuario_permiso DROP CONSTRAINT ck_usuario_permiso_valor;
ALTER TABLE usuario_permiso ADD CONSTRAINT ck_usuario_permiso_valor CHECK (permiso IN (
    'GESTIONAR_CASOS', 'PROGRAMAR_SESIONES', 'CONSULTAR_PARTICIPACIONES',
    'DESCARGAR_REPORTES', 'REGISTRAR_RETROALIMENTACION', 'AUTORIZAR_REPETICION', 'ELIMINAR_CASOS',
    'INVITAR_ESTUDIANTES'));

-- Datos que el estudiante llena al crear su cuenta (el correo viene de la invitación y vive en `usuario`).
CREATE TABLE estudiante_perfil (
    usuario_id       BIGINT       NOT NULL,
    tipo_documento   VARCHAR(5)   NOT NULL,
    numero_documento VARCHAR(20)  NOT NULL,
    fecha_nacimiento DATE         NOT NULL,
    telefono         VARCHAR(20)  NULL,
    acepto_datos_en  DATETIME     NOT NULL,
    creado_en        DATETIME     NOT NULL,
    CONSTRAINT pk_estudiante_perfil PRIMARY KEY (usuario_id),
    CONSTRAINT fk_estudiante_perfil_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT uk_estudiante_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_estudiante_tipo_documento CHECK (tipo_documento IN ('CC', 'TI', 'CE', 'PAS', 'PEP'))
);

-- El token se guarda solo como hash SHA-256: quien lea la base no puede armar el enlace.
CREATE TABLE invitacion_estudiante (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    docente_id    BIGINT       NOT NULL,
    correo        VARCHAR(150) NOT NULL,
    token_hash    CHAR(64)     NOT NULL,
    estado        VARCHAR(12)  NOT NULL DEFAULT 'PENDIENTE',
    creada_en     DATETIME     NOT NULL,
    vence_en      DATETIME     NOT NULL,
    aceptada_en   DATETIME     NULL,
    estudiante_id BIGINT       NULL,
    CONSTRAINT pk_invitacion_estudiante PRIMARY KEY (id),
    CONSTRAINT uk_invitacion_token UNIQUE (token_hash),
    CONSTRAINT fk_invitacion_docente FOREIGN KEY (docente_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitacion_estudiante FOREIGN KEY (estudiante_id) REFERENCES usuario (id) ON DELETE SET NULL,
    CONSTRAINT ck_invitacion_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'CANCELADA'))
);
CREATE INDEX idx_invitacion_docente ON invitacion_estudiante (docente_id, estado);
CREATE INDEX idx_invitacion_correo ON invitacion_estudiante (correo);

-- "Mis estudiantes": quiénes son los estudiantes de cada docente.
CREATE TABLE docente_estudiante (
    docente_id    BIGINT   NOT NULL,
    estudiante_id BIGINT   NOT NULL,
    vinculado_en  DATETIME NOT NULL,
    CONSTRAINT pk_docente_estudiante PRIMARY KEY (docente_id, estudiante_id),
    CONSTRAINT fk_docente_estudiante_docente FOREIGN KEY (docente_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_docente_estudiante_estudiante FOREIGN KEY (estudiante_id) REFERENCES usuario (id) ON DELETE CASCADE
);
CREATE INDEX idx_docente_estudiante_estudiante ON docente_estudiante (estudiante_id);

-- Ecos: estado en vivo (último evento recibido del juego) y repeticiones que concede el docente (+1 intento cada una).
ALTER TABLE ecos_sesion ADD COLUMN ultimo_evento_en DATETIME NULL;

CREATE TABLE ecos_repeticion (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    estudiante_id VARCHAR(64) NOT NULL,
    docente_id    BIGINT      NOT NULL,
    concedida_en  DATETIME    NOT NULL,
    CONSTRAINT pk_ecos_repeticion PRIMARY KEY (id),
    CONSTRAINT fk_ecos_repeticion_docente FOREIGN KEY (docente_id) REFERENCES usuario (id) ON DELETE CASCADE
);
CREATE INDEX idx_ecos_repeticion_estudiante ON ecos_repeticion (estudiante_id);
