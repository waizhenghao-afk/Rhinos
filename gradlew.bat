@echo off
rem Build wrapper: uses the local Gradle 8.10.2 + JDK 21 toolchain set up for this project.
setlocal
set JAVA_HOME=C:\Users\waizh\.ilmucode\workspace\default\.tools\jdk\jdk21
set PATH=%JAVA_HOME%\bin;C:\Users\waizh\.ilmucode\workspace\default\.tools\gradle\gradle-8.10.2\bin;%PATH%
set ANDROID_HOME=C:\Users\waizh\.ilmucode\workspace\default\.tools\android-sdk
cd /d %~dp0
gradle %*
