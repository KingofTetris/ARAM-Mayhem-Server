@echo off
setlocal enabledelayedexpansion

echo ====================================
echo  ARAM Mayhem Server - Local Startup
echo ====================================
echo.

set SERVER_URL=http://localhost:8080
set HEALTH_URL=%SERVER_URL%/actuator/health
set MAX_WAIT=60

echo [1/4] Building project...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed! Check errors above.
    pause
    exit /b 1
)
echo [OK] Build successful.

echo.
echo [2/4] Checking MySQL connection...
mysql -u root -proot -e "SELECT 1;" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [WARN] MySQL connection failed. Make sure MySQL is running on localhost:3306.
    echo        Username: root, Password: root
    echo        Continuing anyway - server may fail to start.
) else (
    echo [OK] MySQL is reachable.
)

echo.
echo [3/4] Checking Redis connection...
redis-cli ping >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [WARN] Redis connection failed. Make sure Redis is running on localhost:6379.
    echo        Continuing anyway - server may fail to start.
) else (
    echo [OK] Redis is reachable.
)

echo.
echo [4/4] Starting server on %SERVER_URL%
echo        Swagger UI: %SERVER_URL%/swagger-ui.html
echo        Health Check: %HEALTH_URL%
echo.
start /b java -jar target\aram-server-1.0.0.jar --spring.profiles.active=local

echo Waiting for server to start (max %MAX_WAIT%s)...
set WAITED=0
:healthcheck
if %WAITED% geq %MAX_WAIT% (
    echo [ERROR] Server did not start within %MAX_WAIT% seconds.
    pause
    exit /b 1
)
timeout /t 2 /nobreak >nul
set /a WAITED+=2

curl -s -o nul -w "%%{http_code}" %HEALTH_URL% 2>nul | findstr "200" >nul
if %ERRORLEVEL% neq 0 (
    echo   ... waiting [%WAITED%s/%MAX_WAIT%s]
    goto healthcheck
)

echo.
echo ====================================
echo  Server started successfully!
echo  URL: %SERVER_URL%
echo  Health: UP
echo  Swagger: %SERVER_URL%/swagger-ui.html
echo ====================================
echo.
echo Press Ctrl+C to stop the server.
echo.

:waitloop
timeout /t 60 /nobreak >nul
curl -s -o nul -w "%%{http_code}" %HEALTH_URL% 2>nul | findstr "200" >nul
if %ERRORLEVEL% neq 0 (
    echo [WARN] Health check failed at %date% %time%
)
goto waitloop
