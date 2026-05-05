@echo off
title Khoi dong He thong QLCSVC-AI
color 0A

echo ========================================================
echo      KHOI DONG HE THONG QLCSVC-AI (Java + Python)
echo ========================================================
echo.

echo [1/2] Dang khoi dong AI API (Python Flask - Port 5000)...
start "AI API (Flask)" cmd /k "python ai_api.py"

echo.
echo [2/2] Dang khoi dong Backend (Spring Boot - Port 8080)...
start "Spring Boot" cmd /k ".\mvnw.cmd spring-boot:run"

echo.
echo ========================================================
echo DA GUI LENH KHOI DONG!
echo.
echo - Co 2 cua so Terminal (mau den) dang chay ngam.
echo - Web se truy cap duoc tai: http://localhost:8080
echo - De tat server, hay tat (bam X) o 2 cua so do.
echo ========================================================
pause
