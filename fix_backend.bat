@echo off
echo Spring Boot Backend Fix Script
echo ==============================
echo.

echo 1. Removing BOM from Java files...
powershell -Command ^
"Get-ChildItem -Path 'src\main\java' -Recurse -Filter '*.java' ^| ForEach-Object { ^
     = [System.IO.File]::ReadAllText(.FullName, [System.Text.Encoding]::UTF8); ^
    if (.StartsWith([char]0xFEFF)) { ^
         = .Substring(1); ^
        [System.IO.File]::WriteAllText(.FullName, , [System.Text.Encoding]::UTF8); ^
        echo Fixed:  ^
    } ^
}"

echo.
echo 2. Testing compilation...
call mvnw.cmd compile -DskipTests

if %ERRORLEVEL% equ 0 (
    echo.
    echo ? COMPILATION SUCCESSFUL!
    echo.
    echo 3. Starting backend...
    echo Backend will run at: http://localhost:8080
    echo Products API: http://localhost:8080/api/v1/products
    echo.
    call mvnw.cmd spring-boot:run
) else (
    echo.
    echo ? COMPILATION FAILED
    echo.
    echo Troubleshooting steps:
    echo 1. Delete all new files created today:
    echo    - Model files (except Book, User, Role, Category)
    echo    - DTO files (except BookDto, ErrorResponse, etc.)
    echo    - Repository files (except BookRepository)
    echo    - Service files (except BookService)
    echo    - Controller files (except BookController, HelloController)
    echo    - handler/ folder
    echo.
    echo 2. Keep only the working files:
    echo    - Security configuration (already works)
    echo    - Book endpoints (already work)
    echo    - JWT authentication (already works)
    echo.
    echo 3. Start with simple ProductController like shown above
)
