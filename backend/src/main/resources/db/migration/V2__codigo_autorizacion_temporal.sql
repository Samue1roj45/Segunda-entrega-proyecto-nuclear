-- TEMPORAL (Módulo 1): códigos de autorización de estudiantes mientras el Módulo 3 (RF-20)
-- publica su propia implementación de VerificadorCodigoAutorizacion. Al integrarse, esta
-- tabla y VerificadorCodigoAutorizacionTemporal se eliminan.
-- El código nunca se guarda en claro: solo su hash BCrypt.
CREATE TABLE codigo_autorizacion_temporal (
    estudiante_id BIGINT       NOT NULL,
    codigo_hash   VARCHAR(100) NOT NULL,
    CONSTRAINT pk_codigo_autorizacion_temporal PRIMARY KEY (estudiante_id),
    CONSTRAINT fk_codigo_autorizacion_temporal_usuario
        FOREIGN KEY (estudiante_id) REFERENCES usuario (id) ON DELETE CASCADE
);
