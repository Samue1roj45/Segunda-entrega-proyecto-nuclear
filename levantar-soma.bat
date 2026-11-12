@echo off
chcp 65001 >nul
title SOMA - reiniciar backend en Docker
rem Doble clic: baja los contenedores, borra la base, reconstruye y levanta backend + MySQL + correo,
rem y espera a que http://localhost:8080/api/salud responda. Requiere Docker Desktop abierto.
setlocal
cd /d "%~dp0"
rem MySQL del contenedor en 3307 para no chocar con un MySQL instalado en el PC
if not defined MYSQL_HOST_PORT set "MYSQL_HOST_PORT=3307"

where docker >nul 2>nul || (echo [ERROR] No se encontro docker. Instale y abra Docker Desktop. & pause & exit /b 1)
docker info >nul 2>nul || (echo [ERROR] Docker Desktop no esta corriendo. Abralo y vuelva a ejecutar. & pause & exit /b 1)

echo === Bajando contenedores y borrando la base ===
docker compose down -v

echo === Construyendo y levantando (la primera vez tarda unos minutos) ===
docker compose up -d --build || (pause & exit /b 1)

echo === Esperando a que el backend responda ===
set /a INTENTOS=0
:esperar
set /a INTENTOS+=1
curl -s http://localhost:8080/api/salud | find "UP" >nul && goto listo
if %INTENTOS% GEQ 90 (echo [ERROR] El backend no respondio. Revise: docker compose logs app & pause & exit /b 1)
timeout /t 5 /nobreak >nul
goto esperar

:listo
echo.
echo ============================================================
echo  SOMA listo:  API http://localhost:8080   Correos http://localhost:8025
echo  Postman: importe la carpeta postman/, entorno "SOMA local" y Run collection.
echo ============================================================
pause
