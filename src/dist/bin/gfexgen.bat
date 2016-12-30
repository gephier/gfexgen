@ECHO OFF

SETLOCAL ENABLEEXTENSIONS

SET JAVA_OPTIONS=-Xms128M -Xmx512M -Xmx1024M

:: run for loop on %~dp0 to get parent directory
for %%i in ("%~dp0..") do set "parent=%%~fi"
SET APP_PATH=%parent%
SET CLASSPATH=%APP_PATH%\conf;%APP_PATH%\lib\*

java %JAVA_OPTIONS% -classpath %CLASSPATH% kr.gfex.main.GfexMain %*
