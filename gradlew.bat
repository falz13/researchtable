@ECHO OFF
SET DIR=%~dp0
SET WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
IF EXIST "%WRAPPER_JAR%" (
  java -jar "%WRAPPER_JAR%" %*
) ELSE (
  gradle %*
)
