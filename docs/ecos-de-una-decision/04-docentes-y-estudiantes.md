# Docentes, estudiantes e invitaciones

## Cómo entra un estudiante
1. El docente (con el permiso **Invitar estudiantes**, que da el administrador en *Docentes*) abre **Mis estudiantes** y escribe los correos institucionales.
2. SOMA manda un correo con un enlace personal (`/registro/<token>`, vale 7 días). El token se guarda solo como hash.
3. El estudiante abre el enlace y llega a **Crea tu cuenta**. El correo viene de la invitación y no se puede cambiar. Llena: nombre completo, tipo y número de documento, fecha de nacimiento (mínimo 14 años), teléfono (opcional), contraseña (8+ caracteres con mayúscula, minúscula y número) y autoriza el tratamiento de datos.
4. Entra con su correo y su contraseña. Queda en la lista del docente.

Si el correo ya tenía cuenta de estudiante, solo se agrega a la lista del docente. Si el correo no llega, el docente ve el enlace y puede copiarlo.

**Retirar** a un estudiante lo saca solo de la lista de ese docente: su cuenta y sus resultados no se borran.

## Ecos de una decisión
- **Intentos:** 1 por defecto (`ECOS_MAX_INTENTOS`); el docente puede dar intentos adicionales a un estudiante suyo (*Dar otro intento*).
- **Nota oficial:** el mejor intento (a igual nota, el primero).
- **Dónde se ve:** Ecos aparece como un caso más en «Casos de estudio» (tarjeta «Videojuego»); al abrirlo el docente tiene las pestañas **En vivo** y **Resultados**. «Mis estudiantes» (invitar, dar intentos, quitar de la lista) es general y está en el menú Docencia.
- **En vivo:** el docente ve quién tiene código, quién juega y en qué acto va (se actualiza cada 5 s).
- **Reanudar desde otro PC:** no se admite (el juego devuelve `reanudar=false`); requiere un cambio en el juego.

## Correo
Por defecto los correos van a Mailpit (`http://localhost:8025`) y **no llegan a bandejas reales**. Para enviar de verdad, definir `SMTP_*` en `.env` (ver `.env.example`) y `FRONTEND_URL` con la dirección pública del frontend.

## Limpiar los datos de casos
`docs/limpiar-datos-de-casos.sql` borra casos, sesiones, convocatorias, participaciones, resultados y avisos; conserva usuarios, invitaciones y todo lo de Ecos. El caso de prueba y su sesión ya no vienen en los datos iniciales de Docker (solo en las pruebas automáticas), así que no reaparecen. Deja a Ana y Luis con contraseña propia (`Clave-Estudiante-01/02`) y vinculados a Carlos.

## Ingreso con Google
Botón único «Continuar con Google» en el login. El navegador obtiene un token de Google y el backend lo verifica (firma, vigencia, que sea de esta aplicación y que el correo esté verificado). Solo entran correos `@unihumboldt.edu.co` y `@cue.edu.co` (`GOOGLE_DOMINIOS`) que **ya tengan cuenta en SOMA** (docente/administrador creado por el admin, o estudiante que aceptó una invitación); no se crean cuentas automáticamente.

Configuración: `GOOGLE_CLIENT_ID` en `.env` (solo el ID de cliente; el «secreto del cliente» no se usa). En Google Cloud, en el cliente OAuth, agrega `http://localhost:4200` (y tu dominio real) en «Orígenes autorizados de JavaScript». Mientras la app esté en modo «Pruebas», solo entran los usuarios de prueba que agregues en la pantalla de consentimiento.
