@echo off
title Flask AI Gateway - QLCSVC
echo.
echo  +-----------------------------------------------+
echo  ^|   Flask AI Gateway - He thong QLCSVC          ^|
echo  ^|   http://localhost:5000                        ^|
echo  +-----------------------------------------------+
echo.

cd /d "%~dp0"

if not exist "venv\Scripts\python.exe" (
    echo [LOI] Khong tim thay venv! Hay chay: python -m venv venv
    echo      Sau do: venv\Scripts\pip install -r requirements.txt
    pause
    exit /b 1
)

echo [OK] Dang khoi dong Flask...
echo [OK] Nhan Ctrl+C de dung server
echo.

venv\Scripts\python.exe ai_api.py

echo.
echo [INFO] Flask da dung.
pause
