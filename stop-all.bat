@echo off
chcp 65001 >nul
powershell -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1"
pause

