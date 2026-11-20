-- Papelera de casos: eliminar un caso lo mueve aquí (borrado lógico) y se puede restaurar.
ALTER TABLE casos_estudio ADD COLUMN fecha_eliminacion DATETIME NULL;
CREATE INDEX idx_casos_eliminacion ON casos_estudio (id_docente, fecha_eliminacion);
