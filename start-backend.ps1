# start-backend.ps1
# Jalankan dengan: .\start-backend.ps1

Write-Host ""
Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   Backend E-Commerce - Dev Server    " -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$envFile = Join-Path $PSScriptRoot ".env"

if (Test-Path $envFile) {
    Write-Host "Membaca konfigurasi dari .env..." -ForegroundColor Yellow
    $lines = Get-Content $envFile
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        $isComment = $trimmed.StartsWith([char]35)
        if ($trimmed.Length -gt 0 -and -not $isComment) {
            $idx = $trimmed.IndexOf("=")
            if ($idx -gt 0) {
                $key   = $trimmed.Substring(0, $idx).Trim()
                $value = $trimmed.Substring($idx + 1).Trim()
                [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
                Write-Host "  OK $key" -ForegroundColor Green
            }
        }
    }
    Write-Host ""
} else {
    Write-Host "PERINGATAN: File .env tidak ditemukan!" -ForegroundColor Red
    Write-Host ""
}

$port = [System.Environment]::GetEnvironmentVariable("SERVER_PORT", "Process")
if (-not $port) { $port = "8081" }

$existingPids = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique

if ($existingPids) {
    foreach ($processId in $existingPids) {
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        $processName = if ($process) { $process.ProcessName } else { "unknown" }
        Write-Host "Port $port dipakai PID $processId ($processName). Mematikan..." -ForegroundColor Yellow
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }

    Start-Sleep -Seconds 2
    Write-Host "Port $port sudah bebas" -ForegroundColor Green
    Write-Host ""
}

Write-Host "Menjalankan Spring Boot di port $port..." -ForegroundColor Cyan
Write-Host ""

$maven = Join-Path $PSScriptRoot "mvnw.cmd"
if (-not (Test-Path $maven)) {
    $maven = "mvn"
}

& $maven spring-boot:run
