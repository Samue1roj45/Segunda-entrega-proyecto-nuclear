-- Índices para las consultas paginadas y los conteos más frecuentes.
CREATE INDEX idx_auditoria_accion ON auditoria (accion, id);
CREATE INDEX idx_practicas_caso ON practicas (id_caso);
CREATE INDEX idx_casos_docente_estado ON casos_estudio (id_docente, estado, fecha_eliminacion);
