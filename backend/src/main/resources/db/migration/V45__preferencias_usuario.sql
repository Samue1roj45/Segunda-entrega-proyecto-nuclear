-- Preferencias de interfaz por usuario (tema, acento, letra...), para que lo acompañen en cualquier navegador.
CREATE TABLE usuario_preferencias (
    usuario_id BIGINT NOT NULL PRIMARY KEY,
    datos TEXT NOT NULL,
    actualizado DATETIME NOT NULL,
    CONSTRAINT fk_usuario_preferencias_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE
);
