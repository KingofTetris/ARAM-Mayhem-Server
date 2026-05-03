@echo off
echo ====================================
echo  ARAM Mayhem Server - Local Startup
echo ====================================
echo.
echo Starting server on http://localhost:8080
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo Health Check: http://localhost:8080/actuator/health
echo.
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo Build failed! Check errors above.
    pause
    exit /b 1
)
java -jar target\aram-server-1.0.0.jar --spring.profiles.active=local
pause
