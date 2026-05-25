@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Maven start up batch script for Windows

@REM User provided maven path or use environment variable
setlocal

if not "%MAVEN_HOME%" == "" goto gotMavenHome

@REM Try to find Maven in Program Files
set "MAVEN_HOME=C:\Program Files\Maven\apache-maven-3.9.8"
if exist "%MAVEN_HOME%\bin\mvn.cmd" goto checkJavaHome
if exist "%MAVEN_HOME%\..\apache-maven-3.9.8\bin\mvn.cmd" (
    set "MAVEN_HOME=%MAVEN_HOME%\..\apache-maven-3.9.8"
    goto checkJavaHome
)

@REM Try local maven
if exist "D:\work\apache-maven-3.9.8\bin\mvn.cmd" (
    set "MAVEN_HOME=D:\work\apache-maven-3.9.8"
    goto checkJavaHome
)

echo Maven not found. Please set MAVEN_HOME environment variable.
exit /b 1

:gotMavenHome
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo ERROR: MAVEN_HOME is set to an invalid directory: %MAVEN_HOME%
    echo Please set the MAVEN_HOME environment variable to a valid Maven installation
    exit /b 1
)

:checkJavaHome
if not "%JAVA_HOME%" == "" goto gotJavaHome

@REM Try JAVA_HOME from environment or default location
if exist "D:\libs\jdks\jdk-21.0.9+10\bin\java.exe" (
    set "JAVA_HOME=D:\libs\jdks\jdk-21.0.9+10"
    goto runMaven
)

echo Please set the JAVA_HOME environment variable to match the location of your Java installation
exit /b 1

:gotJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
    exit /b 1
)

:runMaven
"%MAVEN_HOME%\bin\mvn.cmd" %*

