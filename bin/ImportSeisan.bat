@ECHO OFF

set args=

:Loop
	set arg=%1
	IF "%arg%"=="" GOTO Continue
	SET prefix=%arg:~0,1%

	IF %prefix%==- (
		set "args=%args% %1 %2"
		SHIFT
		SHIFT
	) ELSE (
		set "args=%args% %cd%\%1"
		SHIFT
	)
GOTO Loop

:Continue

cd %~dp0\..
java -cp lib\winston.jar gov.usgs.volcanoes.winston.in.ImportSeisan %args%