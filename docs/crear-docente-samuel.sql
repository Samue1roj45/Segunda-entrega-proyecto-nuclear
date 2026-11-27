-- Crea (o actualiza) al docente Samuel Velásquez Rojas. Repetible: no duplica nada.
-- No tiene contraseña: entra con el botón «Continuar con Google» usando su correo @unihumboldt.edu.co.
INSERT INTO usuario (correo_institucional, nombre_completo, rol, activo)
SELECT 'svelasquez_775@unihumboldt.edu.co', 'Samuel Velásquez Rojas', 'DOCENTE', TRUE
 WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo_institucional = 'svelasquez_775@unihumboldt.edu.co');

UPDATE usuario SET nombre_completo = 'Samuel Velásquez Rojas', rol = 'DOCENTE', activo = TRUE
 WHERE correo_institucional = 'svelasquez_775@unihumboldt.edu.co';

INSERT INTO usuario_permiso (usuario_id, permiso)
SELECT u.id, p.permiso FROM usuario u
  JOIN (SELECT 'GESTIONAR_CASOS' AS permiso UNION ALL SELECT 'PROGRAMAR_SESIONES' UNION ALL SELECT 'CONSULTAR_PARTICIPACIONES'
        UNION ALL SELECT 'DESCARGAR_REPORTES' UNION ALL SELECT 'REGISTRAR_RETROALIMENTACION' UNION ALL SELECT 'AUTORIZAR_REPETICION'
        UNION ALL SELECT 'ELIMINAR_CASOS' UNION ALL SELECT 'INVITAR_ESTUDIANTES') p
 WHERE u.correo_institucional = 'svelasquez_775@unihumboldt.edu.co'
   AND NOT EXISTS (SELECT 1 FROM usuario_permiso x WHERE x.usuario_id = u.id AND x.permiso = p.permiso);
