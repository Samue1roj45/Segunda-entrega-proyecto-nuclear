@echo off
chcp 65001 >nul
title SOMA - backend + frontend
rem Doble clic (o desde la terminal del IDE): levanta interfaz + backend + MySQL + correo en Docker
rem (sin borrar datos) y espera a que la API responda. La interfaz queda en http://localhost:4200.
rem Requiere Docker Desktop abierto. Para recarga automática del frontend: docker compose stop web
rem y luego, en frontend\soma-web, npm ci && npm start (hace falta Node 20+).
setlocal
cd /d "%~dp0"
if not defined MYSQL_HOST_PORT set "MYSQL_HOST_PORT=3307"

where docker >nul 2>nul || (echo [ERROR] No se encontro docker. Instale y abra Docker Desktop. & pause & exit /b 1)
docker info >nul 2>nul || (echo [ERROR] Docker Desktop no esta corriendo. Abralo y vuelva a ejecutar. & pause & exit /b 1)

echo === Levantando interfaz, backend, MySQL y correo (Docker) ===
docker compose up -d --build || (pause & exit /b 1)

echo === Esperando a que la API responda ===
set /a INTENTOS=0
:esperar
set /a INTENTOS+=1
curl -s http://localhost:8080/api/salud | find "UP" >nul && goto api
if %INTENTOS% GEQ 90 (echo [ERROR] El backend no respondio. Revise: docker compose logs app & pause & exit /b 1)
timeout /t 5 /nobreak >nul
goto esperar

:api
echo.
echo  Abra http://localhost:4200
echo  API http://localhost:8080    Correos http://localhost:8025
echo  Docente: carlos.gomez@institucion.edu.co / Clave-Docente-01
echo  Estudiante: ana.perez@institucion.edu.co + codigo CODIGO_VALIDO_01
echo.
pause
