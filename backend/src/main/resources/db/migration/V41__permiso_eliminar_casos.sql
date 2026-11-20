-- Permiso adicional: eliminar casos de estudio. Lo asigna el administrador a cada docente (RF-03).
ALTER TABLE usuario_permiso DROP CONSTRAINT ck_usuario_permiso_valor;
ALTER TABLE usuario_permiso ADD CONSTRAINT ck_usuario_permiso_valor CHECK (permiso IN (
    'GESTIONAR_CASOS', 'PROGRAMAR_SESIONES', 'CONSULTAR_PARTICIPACIONES',
    'DESCARGAR_REPORTES', 'REGISTRAR_RETROALIMENTACION', 'AUTORIZAR_REPETICION', 'ELIMINAR_CASOS'));
