@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

if defined JAVA_HOME (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
    if not exist "%JAVA_HOME%\bin\java.exe" goto fail
) else (
    set "JAVACMD=java.exe"
    where java.exe >NUL 2>&1
    if errorlevel 1 goto fail
)

"%JAVACMD%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
set EXIT_CODE=%ERRORLEVEL%
goto end

:fail
echo.
echo ERROR: JAVA_HOME is not set correctly and java.exe is not available on PATH.
echo.
set EXIT_CODE=1

:end
if "%OS%"=="Windows_NT" endlocal & exit /b %EXIT_CODE%
exit /b %EXIT_CODE%
