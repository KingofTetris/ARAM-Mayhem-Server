@echo off
setlocal enabledelayedexpansion

echo ====================================
echo  ARAM Mayhem - MySQL Backup
echo ====================================
echo.

set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_USER=root
set DB_PASS=root
set DB_NAME=aram_mayhem
set MYSQL_BIN=D:\dev\mysql\bin
set PATH=%MYSQL_BIN%;%PATH%
set BACKUP_DIR=backups
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set BACKUP_FILE=%BACKUP_DIR%\aram_mayhem_%TIMESTAMP%.sql

if not exist %BACKUP_DIR% mkdir %BACKUP_DIR%

echo [1/2] Dumping database %DB_NAME%...
mysqldump -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% --single-transaction --routines --triggers %DB_NAME% > %BACKUP_FILE% 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Backup failed! Check MySQL connection.
    pause
    exit /b 1
)

for %%A in (%BACKUP_FILE%) do set SIZE=%%~zA
echo [OK] Backup created: %BACKUP_FILE% (%SIZE% bytes)

echo.
echo [2/2] Compressing backup...
gzip -f %BACKUP_FILE% 2>nul
if %ERRORLEVEL% equ 0 (
    echo [OK] Compressed: %BACKUP_FILE%.gz
) else (
    echo [INFO] gzip not available, keeping uncompressed backup.
)

echo.
echo Cleaning up backups older than 7 days...
forfiles /p %BACKUP_DIR% /m *.sql.gz /d -7 /c "cmd /c del @path" 2>nul
forfiles /p %BACKUP_DIR% /m *.sql /d -7 /c "cmd /c del @path" 2>nul

echo.
echo ====================================
echo  Backup completed successfully!
echo ====================================
pause
