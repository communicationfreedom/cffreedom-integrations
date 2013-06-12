::#############################################################################
::# Set values in the section below
::#############################################################################
@SET WSDL_URL=http://somedomain/axis/services/USD_R11_WebService?wsdl
@SET STUBS_DIR=com\ca\www\UnicenterServicePlus\ServiceDesk

@SET JAVA_EXE=D:\Dev\java\jdk1.7.0_13\bin\java.exe
@SET JAVAC_EXE=D:\Dev\java\jdk1.7.0_13\bin\javac.exe

@SET JAR_BASE=D:\Dev\java\JARs
@SET AXIS_JAR_DIR=%JAR_BASE%\axis-1_4\lib



::#############################################################################
:: Don't modify anything below here unless you know what you're doing
::#############################################################################

@SET CP=%AXIS_JAR_DIR%\axis.jar;%AXIS_JAR_DIR%\axis-ant.jar;%AXIS_JAR_DIR%\commons-discovery-0.2.jar;%AXIS_JAR_DIR%\commons-logging-1.0.4.jar;%AXIS_JAR_DIR%\jaxrpc.jar;%AXIS_JAR_DIR%\saaj.jar;%AXIS_JAR_DIR%\log4j-1.2.8.jar;%AXIS_JAR_DIR%\wsdl4j-1.5.1.jar;%JAR_BASE%\javax.mail.jar;.

REM Check for USD_WebService.class to see if the stubs have been compiled.
IF EXIST %STUBS_DIR%\USD_WebService.class GOTO COMPILE_APP

REM Check for USD_WebService.java to see if the stubs have been generated.
IF EXIST %STUBS_DIR%\USD_WebService.java  GOTO COMPILE_STUBS

@ECHO.
@ECHO Generating the CA Service Desk Web Services stub files with WSDL2Java
@ECHO.
%JAVA_EXE% -cp %CP% org.apache.axis.wsdl.WSDL2Java -w %WSDL_URL%

::#############################################################################
::# Compile the CA Service Desk stub code.
::#############################################################################
: COMPILE_STUBS
@ECHO.
@ECHO Compiling the CA Service Desk Web Services stub files
@ECHO.
%JAVAC_EXE% -cp %CP% -deprecation %STUBS_DIR%\ArrayOfInt.java
%JAVAC_EXE% -cp %CP% -deprecation %STUBS_DIR%\ArrayOfString.java
%JAVAC_EXE% -cp %CP% -deprecation %STUBS_DIR%\ListResult.java
%JAVAC_EXE% -cp %CP% -deprecation %STUBS_DIR%\USD_WebService.java
%JAVAC_EXE% -cp %CP% -deprecation %STUBS_DIR%\USD_WebServiceLocator.java
%JAVAC_EXE% -cp %CP% -deprecation %STUBS_DIR%\USD_WebServiceSoap.java
%JAVAC_EXE% -cp %CP% -deprecation %STUBS_DIR%\USD_WebServiceSoapSoapBindingStub.java

pause