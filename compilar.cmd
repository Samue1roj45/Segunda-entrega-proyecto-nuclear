@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compilar.ps1"
echo.
echo Resultado guardado en compilar.log
