@echo off
chcp 65001 >nul
title SOMA - backend + frontend
rem Doble clic (o desde la terminal del IDE): levanta backend + MySQL + correo en Docker (sin borrar datos),
rem espera a que la API responda e inicia el frontend en http://localhost:4200
rem Requiere Docker Desktop abierto y Node.js 20+.
setlocal
cd /d "%~dp0"
if not defined MYSQL_HOST_PORT set "MYSQL_HOST_PORT=3307"

where docker >nul 2>nul || (echo [ERROR] No se encontro docker. Instale y abra Docker Desktop. & pause & exit /b 1)
docker info >nul 2>nul || (echo [ERROR] Docker Desktop no esta corriendo. Abralo y vuelva a ejecutar. & pause & exit /b 1)
where npm >nul 2>nul || (echo [ERROR] No se encontro Node.js/npm. Instale Node 20 o superior. & pause & exit /b 1)

echo === Levantando backend (Docker) ===
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
echo === API lista. Iniciando frontend ===
cd frontend\soma-web
if not exist node_modules (call npm ci || (pause & exit /b 1))
echo.
echo  Abra http://localhost:4200
echo  Docente: carlos.gomez@institucion.edu.co / Clave-Docente-01
echo  Estudiante: ana.perez@institucion.edu.co + codigo CODIGO_VALIDO_01
echo.
call npm start
