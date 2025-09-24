@ECHO OFF
SET SCRIPT_DIR=%~dp0
WHERE gradle >NUL 2>&1
IF %ERRORLEVEL% EQU 0 (
  gradle -p %SCRIPT_DIR% %*
  EXIT /B %ERRORLEVEL%
) ELSE (
  ECHO Gradle is required to build this project. Install Gradle 8.x or add it to PATH.
  EXIT /B 1
)
