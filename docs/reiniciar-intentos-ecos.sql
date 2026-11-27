-- Reinicia las participaciones y los intentos de «Ecos de una decisión» para volver a probarlo desde cero.
-- Borra: participaciones, respuestas, resultados y notas del caso Ecos, y los intentos/resultados/repeticiones del juego.
-- NO borra: el caso, sus sesiones programadas, las convocatorias de los estudiantes, usuarios ni el catálogo de Ecos.
-- Es repetible. Después, el estudiante vuelve a ver «Intentos 0 de 1» y puede jugar de nuevo.
--
-- Cómo correrlo (desde la raíz del proyecto, con la base levantada):
--   PowerShell:  Get-Content docs\reiniciar-intentos-ecos.sql -Raw | docker compose exec -T db mysql -u plataforma -pcambiar-esta-clave plataforma_casos

SET FOREIGN_KEY_CHECKS = 0;

-- Participaciones del caso Ecos en la plataforma web
DELETE FROM notificacion        WHERE participacion_id IN (SELECT id FROM participacion WHERE practica_id IN (SELECT id_practica FROM practicas WHERE id_caso IN (SELECT id_caso FROM casos_estudio WHERE nombre = 'Ecos de una decisión')));
DELETE FROM retroalimentacion   WHERE participacion_id IN (SELECT id FROM participacion WHERE practica_id IN (SELECT id_practica FROM practicas WHERE id_caso IN (SELECT id_caso FROM casos_estudio WHERE nombre = 'Ecos de una decisión')));
DELETE FROM nota_oficial        WHERE participacion_id IN (SELECT id FROM participacion WHERE practica_id IN (SELECT id_practica FROM practicas WHERE id_caso IN (SELECT id_caso FROM casos_estudio WHERE nombre = 'Ecos de una decisión')));
DELETE FROM resultado           WHERE participacion_id IN (SELECT id FROM participacion WHERE practica_id IN (SELECT id_practica FROM practicas WHERE id_caso IN (SELECT id_caso FROM casos_estudio WHERE nombre = 'Ecos de una decisión')));
DELETE FROM respuesta_seleccionada WHERE participacion_id IN (SELECT id FROM participacion WHERE practica_id IN (SELECT id_practica FROM practicas WHERE id_caso IN (SELECT id_caso FROM casos_estudio WHERE nombre = 'Ecos de una decisión')));
DELETE FROM participacion       WHERE practica_id IN (SELECT id_practica FROM practicas WHERE id_caso IN (SELECT id_caso FROM casos_estudio WHERE nombre = 'Ecos de una decisión'));

-- Intentos del juego
DELETE FROM ecos_resultado_criterio;
DELETE FROM ecos_respuesta;
DELETE FROM ecos_resultado;
DELETE FROM ecos_sesion;
DELETE FROM ecos_repeticion;

SET FOREIGN_KEY_CHECKS = 1;
