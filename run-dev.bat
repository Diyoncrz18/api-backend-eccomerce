@echo off
setlocal EnableExtensions EnableDelayedExpansion
:: ============================================================
:: Script untuk menjalankan backend dengan variabel dari .env
:: Cara pakai: klik 2x file ini atau jalankan di terminal
:: ============================================================

echo Membaca konfigurasi dari .env...

if exist ".env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
        if not "%%~A"=="" if not "%%~B"=="" (
            set "%%~A=%%~B"
        )
    )
) else (
    echo PERINGATAN: File .env tidak ditemukan.
)

if not defined SERVER_PORT set "SERVER_PORT=8081"

echo Konfigurasi berhasil dimuat!
echo Database: %DB_USERNAME%@%DB_HOST%:%DB_PORT%/%DB_NAME%
echo Server port: %SERVER_PORT%
echo.

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%SERVER_PORT% .*LISTENING"') do (
    echo Port %SERVER_PORT% masih dipakai oleh PID %%P. Mematikan...
    taskkill /PID %%P /F >nul 2>nul
)

echo Menjalankan Spring Boot...
if exist "mvnw.cmd" (
    call mvnw.cmd spring-boot:run
) else (
    call mvn spring-boot:run
)

pause
