@echo off
setlocal

set SERVER_URL=http://localhost:8080
set HEALTH_URL=%SERVER_URL%/actuator/health

echo Checking ARAM Mayhem Server health...
echo.

curl -s %HEALTH_URL% 2>nul
if %ERRORLEVEL% neq 0 (
    echo.
    echo [FAIL] Server is not reachable at %SERVER_URL%
    exit /b 1
)

echo.
echo.

curl -s -o nul -w "HTTP Status: %%{http_code}\n" %HEALTH_URL% 2>nul | findstr "200" >nul
if %ERRORLEVEL% equ 0 (
    echo [OK] Server is healthy.
) else (
    echo [WARN] Server responded but health check did not return 200.
)

echo.
echo Endpoints:
echo   Swagger UI: %SERVER_URL%/swagger-ui.html
echo   API Docs:   %SERVER_URL%/v3/api-docs
echo   Health:     %HEALTH_URL%
