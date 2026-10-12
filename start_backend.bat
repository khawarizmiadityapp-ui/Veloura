@echo off
title Veloura Audio & Lyrics Backend
cls
echo ========================================================
echo       VELOURA AUDIO STREAMING ^& LYRICS SERVER
echo ========================================================
echo.
echo Pastikan koneksi internet aktif.
echo Server akan mendengarkan request audio stream dan lirik.
echo.
cd /d "%~dp0backend"
echo [INFO] Menjalankan server pada port 5000...
echo [INFO] Tekan Ctrl+C untuk menghentikan server.
echo.
python server.py
pause
