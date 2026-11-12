-- Datos FICTICIOS (perfiles "pruebas" y "test"): un caso PUBLICADO de Carlos Gómez para poder programar
-- la sesión de prueba (módulos 3 y 4) y evaluarla (módulos 5 y 6). Cumple las reglas OCL del módulo 2.

INSERT INTO casos_estudio (id_docente, nombre, desarrollo_situacional, contexto_historia, tiempo_estimado_min,
                           estado, fecha_creacion, fecha_actualizacion, fecha_publicacion)
SELECT id, 'Conformidad en el grupo de trabajo',
       'Un equipo nuevo debe decidir cómo repartir las tareas de un proyecto con plazo corto.',
       'Marta acaba de llegar a la empresa y nota que nadie contradice al líder del equipo.',
       60, 'PUBLICADO', '2026-08-03 08:00:00', '2026-08-03 08:00:00', '2026-08-03 08:00:00'
  FROM usuario WHERE correo_institucional = 'carlos.gomez@institucion.edu.co';

INSERT INTO escenarios (id_caso, nombre, contexto, orden)
SELECT id_caso, 'Primera reunión', 'El líder propone un plan que tiene un error evidente.', 1
  FROM casos_estudio WHERE nombre = 'Conformidad en el grupo de trabajo';

INSERT INTO preguntas (id_escenario, enunciado, orden)
SELECT e.id_escenario, '¿Qué fenómeno explica que nadie señale el error?', 1
  FROM escenarios e JOIN casos_estudio c ON c.id_caso = e.id_caso
 WHERE c.nombre = 'Conformidad en el grupo de trabajo';

INSERT INTO preguntas (id_escenario, enunciado, orden)
SELECT e.id_escenario, '¿Qué debería hacer Marta para que el equipo revise el plan?', 2
  FROM escenarios e JOIN casos_estudio c ON c.id_caso = e.id_caso
 WHERE c.nombre = 'Conformidad en el grupo de trabajo';

INSERT INTO respuestas (id_pregunta, codigo, texto, es_correcta, porcentaje, justificacion)
SELECT p.id_pregunta, 'A', 'Conformidad normativa', TRUE, 100,
       'El grupo evita el rechazo social y acepta la propuesta del líder.'
  FROM preguntas p WHERE p.enunciado = '¿Qué fenómeno explica que nadie señale el error?';
INSERT INTO respuestas (id_pregunta, codigo, texto, es_correcta, porcentaje, justificacion)
SELECT p.id_pregunta, 'B', 'Facilitación social', FALSE, 0,
       'La facilitación social mejora el rendimiento en tareas simples; no explica el silencio.'
  FROM preguntas p WHERE p.enunciado = '¿Qué fenómeno explica que nadie señale el error?';
INSERT INTO respuestas (id_pregunta, codigo, texto, es_correcta, porcentaje, justificacion)
SELECT p.id_pregunta, 'A', 'Expresar su duda en la reunión con datos', TRUE, 100,
       'Una voz disidente con argumentos reduce la presión de conformidad del resto.'
  FROM preguntas p WHERE p.enunciado = '¿Qué debería hacer Marta para que el equipo revise el plan?';
INSERT INTO respuestas (id_pregunta, codigo, texto, es_correcta, porcentaje, justificacion)
SELECT p.id_pregunta, 'B', 'Esperar a que el plan falle', FALSE, 0,
       'Esperar aumenta el costo del error y no cambia la dinámica del grupo.'
  FROM preguntas p WHERE p.enunciado = '¿Qué debería hacer Marta para que el equipo revise el plan?';

INSERT INTO rubricas (id_caso, nombre, descripcion)
SELECT id_caso, 'Rúbrica de análisis', 'Identificación del fenómeno y propuesta de intervención.'
  FROM casos_estudio WHERE nombre = 'Conformidad en el grupo de trabajo';

INSERT INTO criterios_evaluacion (id_rubrica, criterio, parametros, rango_min, rango_max)
SELECT r.id_rubrica, 'Análisis del caso', 'Identifica el fenómeno y propone una intervención', 0, 5
  FROM rubricas r JOIN casos_estudio c ON c.id_caso = r.id_caso
 WHERE c.nombre = 'Conformidad en el grupo de trabajo';
