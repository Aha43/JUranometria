@echo off
rem JUranometria launcher: runs from its own directory (spaces are
rem fine), checks for Java, and hands over to java -jar - which
rem remains the authoritative launch path if this helper is
rem unsuitable.
setlocal
set "DIR=%~dp0"
where java >nul 2>nul
if errorlevel 1 (
    echo JUranometria needs a Java runtime, version 21 or later.
    echo None was found on the PATH. Install one, for example from
    echo https://adoptium.net, and try again.
    exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "RAWVER=%%~v"
set "MAJOR=%RAWVER:~0,2%"
if "%MAJOR:~1,1%"=="." set "MAJOR=%RAWVER:~0,1%"
set /a MAJORNUM=%MAJOR% 2>nul
if %MAJORNUM% LSS 21 (
    echo JUranometria needs Java 21 or later.
    java -version
    echo Install Java 21+, for example from https://adoptium.net.
    exit /b 1
)
java --enable-native-access=ALL-UNNAMED -jar "%DIR%JUranometria.jar" %*
endlocal
