@echo off
REM ===========================================================================
REM PostgreSQL Database Management Script
REM ===========================================================================
REM This script provides simple commands to manage the Docker database container.
REM 
REM Usage:
REM   manage-db.bat up      - Starts the database in the background.
REM   manage-db.bat down    - Stops and removes the database container.
REM   manage-db.bat status  - Shows the status of the container.
REM   manage-db.bat logs    - Follows the database logs.
REM ===========================================================================

pushd "%~dp0"
set COMMAND=%1

if "%COMMAND%"=="up" (
    echo [INFO] Starting PostgreSQL database...
    docker-compose up -d
    goto :cleanup
)

if "%COMMAND%"=="down" (
    echo [INFO] Stopping PostgreSQL database...
    docker-compose down
    goto :cleanup
)

if "%COMMAND%"=="status" (
    echo [INFO] Database status:
    docker-compose ps
    goto :cleanup
)

if "%COMMAND%"=="logs" (
    echo [INFO] Showing logs (Ctrl+C to exit)...
    docker-compose logs -f
    goto :cleanup
)

REM Default help message
echo [ERROR] Invalid command: "%COMMAND%"
echo.
echo Usage: manage-db.bat [up^|down^|status^|logs]
echo   up     : Start the database container (detached)
echo   down   : Stop and remove the container
echo   status : Check if the container is running
echo   logs   : View real-time database logs

:cleanup
popd
goto :eof
