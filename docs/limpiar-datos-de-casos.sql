-- Limpieza de los datos de CASOS de estudio (casos, escenarios, preguntas, rúbricas, sesiones, convocatorias,
-- participaciones, resultados, notas, retroalimentación y los avisos de la campana).
-- NO toca: usuarios y sus permisos, perfiles de estudiantes, invitaciones, auditoría, preferencias ni nada de Ecos.
-- Es repetible: si se corre dos veces, la segunda no cambia nada.
--
-- Cómo correrlo (desde la raíz del proyecto, con la base levantada y el backend ya reconstruido):
--   PowerShell:  Get-Content docs\limpiar-datos-de-casos.sql -Raw | docker compose exec -T db mysql -u plataforma -pcambiar-esta-clave plataforma_casos
--
-- Al borrar las convocatorias, Ana y Luis dejan de tener el código de sesión como contraseña. Para que sigan pudiendo
-- entrar a probar Ecos, este script les deja una contraseña propia y los vincula con el docente de ejemplo (Carlos):
--   ana.perez@institucion.edu.co   ->  Clave-Estudiante-01
--   luis.rios@institucion.edu.co   ->  Clave-Estudiante-02

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM notificacion;
DELETE FROM retroalimentacion;
DELETE FROM nota_oficial;
DELETE FROM resultado;
DELETE FROM respuesta_seleccionada;
DELETE FROM participacion;
DELETE FROM autorizaciones_repeticion;
DELETE FROM autorizaciones;
DELETE FROM herramientas_caso;
DELETE FROM practicas;
DELETE FROM criterios_evaluacion;
DELETE FROM rubricas;
DELETE FROM respuestas;
DELETE FROM preguntas;
DELETE FROM escenarios;
DELETE FROM casos_estudio;
DELETE FROM aviso;

SET FOREIGN_KEY_CHECKS = 1;

-- Estudiantes de ejemplo: contraseña propia (BCrypt) para que puedan entrar sin una convocatoria.
UPDATE usuario SET clave_hash = '$2a$10$rVPAHXoMhvhmyKcGj4hiHu8C8dyjXL19I8kh257U75DeeOnm9xj4S'
 WHERE correo_institucional = 'ana.perez@institucion.edu.co' AND rol = 'ESTUDIANTE';
UPDATE usuario SET clave_hash = '$2a$10$oFS7xCNCpzIkPhwOb35Y7.ttfRo8M0GHjLwGyWChEV5UgtSFNGxLe'
 WHERE correo_institucional = 'luis.rios@institucion.edu.co' AND rol = 'ESTUDIANTE';

-- El docente de ejemplo puede invitar y ver a sus estudiantes, y ya tiene a Ana y Luis en su lista.
INSERT INTO usuario_permiso (usuario_id, permiso)
SELECT u.id, 'INVITAR_ESTUDIANTES' FROM usuario u
 WHERE u.correo_institucional = 'carlos.gomez@institucion.edu.co'
   AND NOT EXISTS (SELECT 1 FROM usuario_permiso p WHERE p.usuario_id = u.id AND p.permiso = 'INVITAR_ESTUDIANTES');

INSERT INTO docente_estudiante (docente_id, estudiante_id, vinculado_en)
SELECT d.id, e.id, NOW() FROM usuario d, usuario e
 WHERE d.correo_institucional = 'carlos.gomez@institucion.edu.co'
   AND e.correo_institucional IN ('ana.perez@institucion.edu.co', 'luis.rios@institucion.edu.co')
   AND NOT EXISTS (SELECT 1 FROM docente_estudiante x WHERE x.docente_id = d.id AND x.estudiante_id = e.id);
