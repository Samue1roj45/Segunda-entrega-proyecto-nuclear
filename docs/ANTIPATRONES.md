# SOMA · Revisión de antipatrones del backend

Revisión hecha sobre el backend único (`aplicacion` + `modulo-1`, `modulo-2`, `modulo-3-4`, `modulo-5-6`) con
274 pruebas automáticas en verde (H2) y 18 pruebas contra MySQL 8 en verde.

| Antipatrón | Estado | Evidencia |
|---|---|---|
| God Object / clase gigante | Evitado | La clase más grande es `CasoEstudio` (≈260 líneas, entidad raíz del agregado). Los servicios no pasan de ≈210 líneas y cada uno atiende un solo RF o un grupo pequeño. |
| Código duplicado entre módulos | Corregido | M2 traía una copia completa de M1 y M3-4 traía su propio verificador temporal y migraciones V2/V2_1 de usuarios: se eliminaron. Hay una sola autenticación, un solo `ProxyAcceso` y un solo `ManejadorErroresApi`. |
| Magic strings para estados | Corregido | Los estados de sesión y autorización usan las constantes `Practica.SIN_INICIAR/EN_CURSO/FINALIZADO` y `Autorizacion.ACTIVA/RETIRADA`; los de intento usan el enum `EstadoParticipacion`. |
| Dependencias circulares | Corregido | Había un ciclo de beans `CatalogoAcademicoIntegrado → ServicioConsultaSesiones → RepeticionRepositorio → ServicioRegistroParticipaciones → CatalogoAcademico`. Se rompió moviendo el conteo a `AutorizacionRepositorio`. Los módulos se hablan sólo por puertos (ver `PATRONES.md`). |
| Acoplamiento directo entre módulos | Evitado | M5 no importa clases de M3: depende de sus puertos `contrato/*`, y los adaptadores viven en `aplicacion/integracion`. M1 no conoce a M3: define `VerificadorCodigoAutorizacion`. |
| Credenciales en el código | Evitado | Contraseñas de BD, correo y el secreto JWT salen de variables de entorno (`DB_PASSWORD`, `MAIL_PASSWORD`, `JWT_SECRETO`); `.env.example` sólo trae valores de ejemplo. Los códigos de acceso se guardan con BCrypt. |
| Entidades expuestas en la API | Evitado | Ningún `@RestController` devuelve entidades JPA; todos responden DTO/records (`*Respuesta`, `*DTO`). |
| Inyección por campo / Service Locator | Evitado | Toda la inyección es por constructor. No hay `@Autowired` en campos. |
| Errores tragados | Evitado | No hay `catch` vacíos, `printStackTrace` ni `System.out`. Los errores de negocio salen como `ErrorApi {fecha, estado, error, mensaje, ruta}` desde un único manejador. |
| Arrow code / if anidados por estado | Evitado | Las reglas por estado usan State; las validaciones encadenadas usan Chain of Responsibility; la regla de ingreso tardío y el cálculo de nota usan Strategy. |
| Golden Hammer | Evitado | Cada patrón se usa donde el RF lo pide. M2 deja a propósito un `enum` para el estado del caso (dos estados) en vez de forzar State. |
| Lava flow / código muerto | Corregido | Se borraron el verificador temporal, migraciones duplicadas y la copia de M1 dentro de M2. |
| Versiones de tecnología inconsistentes | Corregido | Un único `pom.xml`: Java 21, Spring Boot 4.1.2, OpenPDF 2.0.3, Apache POI 5.4.1, MySQL 8 con Flyway por rangos de versión por módulo. |
| Nombres inconsistentes | Corregido | Paquete raíz único `co.edu.humboldt.plataformacasos`; nombre del producto SOMA en backend, frontends y documentación. |
