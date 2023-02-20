@echo off

set _DEBUG=1

:: И ещё — в пару переменных забьём наши пакеты и классы.
:: Если заходите их сменить — вам не придётся
:: бегать по коду — все настройки вначале.

if defined _DEBUG echo ___ set-app-env.cmd ____________________________________
if defined _DEBUG echo.

set project_dir=%cd%
if defined _DEBUG echo project_dir = %project_dir%

set AUTHOR_NAMESPACE=slairium
if defined _DEBUG echo AUTHOR_NAMESPACE = %AUTHOR_NAMESPACE%

for /F "delims=" %%i in ("%project_dir%") do set APK_NAME=%%~nxi
if defined _DEBUG echo APK_NAME = %APK_NAME%

set PACKAGE_PATH=com\%AUTHOR_NAMESPACE%\%APK_NAME%
if defined _DEBUG echo PACKAGE_PATH = %PACKAGE_PATH%

set PACKAGE_PATH1=fc\cron
if defined _DEBUG echo PACKAGE_PATH1 = %PACKAGE_PATH1%

set PACKAGE=com.%AUTHOR_NAMESPACE%.%APK_NAME%
if defined _DEBUG echo PACKAGE = %PACKAGE%

set MAIN_CLASS=MainActivity
if defined _DEBUG echo MAIN_CLASS = %MAIN_CLASS%

if defined _DEBUG echo ________________________________________________________
if defined _DEBUG echo.
