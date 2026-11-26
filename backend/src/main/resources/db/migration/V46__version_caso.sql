-- Versión del caso: un guardado atrasado no reemplaza en silencio otro más reciente.
ALTER TABLE casos_estudio ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
