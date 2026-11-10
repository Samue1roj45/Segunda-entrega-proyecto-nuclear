-- Datos FICTICIOS para ejecutar los casos de prueba del Módulo 1 (perfil "pruebas").
-- No son credenciales reales. Los códigos de autorización los genera el Módulo 3 (RF-20).

INSERT INTO usuario (correo_institucional, nombre_completo, rol) VALUES
    ('admin.plataforma@institucion.edu.co', 'Administrador de la Plataforma', 'ADMINISTRADOR'),
    ('carlos.gomez@institucion.edu.co',     'Carlos Gómez',                   'DOCENTE'),
    ('laura.mejia@institucion.edu.co',      'Laura Mejía',                    'DOCENTE'),
    ('ana.perez@institucion.edu.co',        'Ana Pérez',                      'ESTUDIANTE'),
    ('luis.rios@institucion.edu.co',        'Luis Ríos',                      'ESTUDIANTE');

-- Carlos tiene GESTIONAR_CASOS (CP-RF02-03, CP-RF03-05). Laura no tiene permisos (CP-RF02-04, CP-RF03-04).
INSERT INTO usuario_permiso (usuario_id, permiso)
SELECT id, 'GESTIONAR_CASOS' FROM usuario WHERE correo_institucional = 'carlos.gomez@institucion.edu.co';
