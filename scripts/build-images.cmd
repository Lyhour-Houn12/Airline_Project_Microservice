@echo off
setlocal enabledelayedexpansion

rem Builds the Docker images of the services with Jib (no Dockerfile). Windows version of build-images.sh.
rem
rem Usage (from anywhere):
rem   scripts\build-images.cmd                                  all services, one after another, into the local Docker
rem   scripts\build-images.cmd booking-service seat-service     only the named services
rem   scripts\build-images.cmd --parallel                       all services at the same time (logs in target\build-logs)
rem   scripts\build-images.cmd -p booking-service seat-service  named services at the same time
rem   set IMAGE_PREFIX=<docker-hub-user> ^& scripts\build-images.cmd --push
rem                                                             build, then `docker push` each image (needs `docker login`)
rem   scripts\build-images.cmd -p --push booking-service        flags and service names combine freely
rem
rem Images: <prefix>/<service>:latest. The prefix is <docker.image.prefix> in the root pom.xml
rem ("lyhour121"), or IMAGE_PREFIX. The CPU type is arm64; JIB_ARCH=amd64 builds for ordinary servers.

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "LOG_DIR=%PROJECT_ROOT%\target\build-logs"
set "DEFAULT_SERVICES=discovery-server api-gateway user-service airline-core-service location-service flight-ops-service pricing-service seat-service ancillary-service booking-service payment-service"
set "PARALLEL=0"
set "PUSH=0"
set "ARGS="

for %%A in (%*) do (
    if /I "%%A"=="--parallel" (
        set "PARALLEL=1"
    ) else if /I "%%A"=="-p" (
        set "PARALLEL=1"
    ) else if /I "%%A"=="--push" (
        set "PUSH=1"
    ) else (
        set "ARGS=!ARGS! %%A"
    )
)
set "SERVICES=%DEFAULT_SERVICES%"
if not "!ARGS!"=="" set "SERVICES=!ARGS!"

rem Image prefix: IMAGE_PREFIX, else <docker.image.prefix> from the root pom.xml
if defined IMAGE_PREFIX (
    set "PREFIX=%IMAGE_PREFIX%"
) else (
    for /f "usebackq tokens=2 delims=<>" %%X in (`findstr /C:"<docker.image.prefix>" "%PROJECT_ROOT%\pom.xml"`) do set "PREFIX=%%X"
)
set "MVN_FLAGS=-B -q -DskipTests -Ddocker.image.prefix=!PREFIX!"
if defined JIB_ARCH set "MVN_FLAGS=!MVN_FLAGS! -Djib.arch=%JIB_ARCH%"

rem Check every name before building anything
for %%S in (!SERVICES!) do (
    call :module_path %%S
    if "!MODULE!"=="" (
        echo Unknown service: %%S
        echo Known: %DEFAULT_SERVICES% config-server
        exit /b 2
    )
)

set "ACTION=built"
if "!PUSH!"=="1" set "ACTION=built and pushed"

rem Every service depends on common-lib: build and install it (and the parent poms) once, first,
rem so the service builds don't each rebuild it (in parallel they would overwrite each other's files).
echo ==^> Building common-lib
pushd "%PROJECT_ROOT%"
call mvn -B -q -DskipTests -pl common-lib -am install
if errorlevel 1 (
    popd
    echo ==^> common-lib failed to build
    exit /b 1
)
popd

if "!PARALLEL!"=="1" (
    call :build_parallel
) else (
    call :build_sequential
)
exit /b %errorlevel%

rem services\<name> or cloud\<name>; leaves the result in MODULE (empty if unknown)
:module_path
set "MODULE="
if exist "%PROJECT_ROOT%\services\%~1\pom.xml" set "MODULE=services/%~1"
if exist "%PROJECT_ROOT%\cloud\%~1\pom.xml" set "MODULE=cloud/%~1"
goto :eof

:build_sequential
for %%S in (!SERVICES!) do (
    echo ==^> Building image for %%S
    call :module_path %%S
    pushd "%PROJECT_ROOT%"
    call mvn !MVN_FLAGS! -pl !MODULE! package jib:dockerBuild
    if errorlevel 1 (
        popd
        echo ==^> Build failed for %%S
        exit /b 1
    )
    popd
    if "!PUSH!"=="1" (
        echo ==^> Pushing !PREFIX!/%%S
        docker push --all-tags "!PREFIX!/%%S"
        if errorlevel 1 (
            echo ==^> Push failed for %%S
            exit /b 1
        )
    )
)
echo ==^> All images %ACTION%: !SERVICES!
exit /b 0

:build_parallel
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

for %%S in (!SERVICES!) do (
    call :module_path %%S
    if exist "%LOG_DIR%\%%S.done" del "%LOG_DIR%\%%S.done"
    > "%LOG_DIR%\%%S.worker.cmd" (
        echo @echo off
        echo cd /d "%PROJECT_ROOT%"
        echo call mvn !MVN_FLAGS! -pl !MODULE! package jib:dockerBuild ^> "%LOG_DIR%\%%S.log" 2^>^&1
        if "!PUSH!"=="1" echo if not errorlevel 1 docker push --all-tags "!PREFIX!/%%S" ^>^> "%LOG_DIR%\%%S.log" 2^>^&1
        echo echo %%errorlevel%% ^> "%LOG_DIR%\%%S.done"
    )
    echo ==^> Starting build for %%S (log: target\build-logs\%%S.log)
    start "build-%%S" /B cmd /c "%LOG_DIR%\%%S.worker.cmd"
)

:waitloop
set "PENDING=0"
for %%S in (!SERVICES!) do (
    if not exist "%LOG_DIR%\%%S.done" set "PENDING=1"
)
if "!PENDING!"=="1" (
    timeout /t 2 /nobreak >nul
    goto waitloop
)

set "FAILED="
for %%S in (!SERVICES!) do (
    set /p RESULT=<"%LOG_DIR%\%%S.done"
    set "RESULT=!RESULT: =!"
    if not "!RESULT!"=="0" set "FAILED=!FAILED! %%S"
)

if "!FAILED!"=="" (
    echo ==^> All images %ACTION% successfully: !SERVICES!
    exit /b 0
) else (
    echo ==^> Build FAILED for:!FAILED!
    echo     See logs in "%LOG_DIR%"
    exit /b 1
)
