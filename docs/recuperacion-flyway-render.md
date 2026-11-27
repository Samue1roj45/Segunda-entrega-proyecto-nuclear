# Recuperación de Flyway en Render (migración 35.1 fallida)

**Síntoma:** el despliegue no arranca y el log dice `Migration of schema ... to version "35.1 - Sesion prueba" failed!`
con `Illegal operation on empty result set`. En los siguientes despliegues aparece
`Detected failed migration to version 35.1`.

**Causa:** `V35_1__Sesion_prueba` buscaba el caso de prueba que otra migración sembraba, y ese caso ya no existe. Al no
insertar nada, MySQL falla al leer la llave generada. En el equipo local no se veía porque `target/classes` conservaba
una copia vieja del archivo del caso (con `mvn clean` también falla). MySQL no revierte los cambios de estructura, así que
Flyway deja la fila de la migración con `success = 0` y se niega a seguir.

**Corrección (ya en `main`):** `V35_1` ya no depende de ese caso. La sesión de prueba (Carlos con Ana y Luis, códigos
`CODIGO_VALIDO_01/02`) y el caso «Ecos de una decisión» los siembra `SemillaEcosPruebas` al arrancar con el perfil `pruebas`.

## Pasos en Render (base con la migración fallida)

1. En el servicio del backend, **Environment**, agrega `FLYWAY_REPARAR = true` (déjala solo para este despliegue).
2. Despliega la última versión de `main` (**Manual Deploy → Deploy latest commit**).
3. En el log debe verse: `FLYWAY_REPARAR=true: reparando el historial...`, `Successfully repaired schema history table` y
   `Successfully applied N migrations ... now at version v48`.
4. Cuando arranque bien, **borra** la variable `FLYWAY_REPARAR` y vuelve a desplegar. El *repair* solo elimina las filas
   fallidas del historial y realinea sumas de verificación: no toca tus tablas ni tus datos.

## Alternativa manual (sin variable)

Con acceso a la base: `DELETE FROM flyway_schema_history WHERE success = 0;` y vuelve a desplegar.

## Base vacía (empezar de cero)

Si no hay nada que conservar, es más simple borrar la base y desplegar: se aplican las 22 migraciones, se cargan los
5 usuarios originales (admin, Carlos, Laura, Ana y Luis), el banco de Ecos, el caso publicado y la sesión de prueba.

## Verificación hecha

MySQL 8.0, base vacía, perfil `pruebas`: 22 de 22 migraciones correctas, los 5 usuarios entran (Ana y Luis con su código),
y el estado «35.1 fallida» se recupera con `FLYWAY_REPARAR=true` (sin la variable, el arranque se detiene como debe).
