@echo off
setlocal

set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if not exist "%CLASSPATH%" (
    echo Gradle wrapper JAR not found at %CLASSPATH%
    echo Run the project in Android Studio or install Gradle wrapper jar manually.
    exit /b 1
)

if defined JAVA_HOME (
    set JAVA_CMD=%JAVA_HOME%\bin\java.exe
) else (
    set JAVA_CMD=java.exe
)

"%JAVA_CMD%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
