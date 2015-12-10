# Winston Wave Server

Winston Wave Server is used to serve data to Swarm and web browsers.

--- 

## Configuring Winston

When WWS starts it will look for a configuration file called WWS.config in the current working directory. A different location may be specified in the last argument on the command line.

The configuration file in an unordered list of `key=value` pairs.

### database keys
These settings are used by most winston applications. To avoid setting these values in multiple files it's common to place them in a Winston.config file that in included in WWS.config using `@include Winston.config`

- winston.driver -- _required_ The fully qualified class name for the database driver to use to connect to Winston.  Most likely you'll never have to change this.
- winston.url -- _required_ The JDBC URL used to connect to the Winston database.
- winston.prefix -- _required_ The prefix on all of the Winston databases. Multiple Winstons may share the same MySQL instance provided each has a unique prefix.

### WWS keys
- wws.port -- _required_ The port WWS will bind to.
- wws.keepalive -- _optional_ If true, `SO_KEEPALIVE` will be set on accepted connections. Helpful when transiting firewalls.
- wws.handlers -- _required_ The number of server handlers. Server handlers receive requests from clients and fill them by requesting data from the database. Most servers run well with 4 handlers. Excessive handlers will make inefficent use of system resources, reducing the overall number of requests the server can fill. Wave Servers which serve many HTTP plots or clients across slow networks may benefit from additional handlers.
- wws.maxConnections -- _required_ The maximum number of connections that the Wave Server will maintain. Unlike handlers this number can frequently be safly increased.
- wws.idleTime -- _optional_ The length of time, in seconds, that a connection can remain quiet before WWS will consider it idle. Used when dropping idle connections to free resources.
- www.allowHttp -- _required_ If true, the Wave Server will respond to requests from web browsers. If false, only the wave server protocol will be supported.
- wws.httpMaxSize -- _optional_ An integer value that specifies whether the maximum product of requested width and height WWS should respond to HTTP GET requests.
- wws.maxDays -- _required_ The maximum age of data, in days, that will be returned to a client. If 0, all data will be available to fill client requests. Used to permit multiple WWS instances to feed from a single database while presenting different apparent retention policies.
- wws-slowCommandTime -- _optional_ The length of time, in milliseconds, a command can run before being logged as slow command.

## Launching Winston
Start Winston Wave Server with a command similar to <code>java -cp lib/winston.jar gov.usgs.volcanoes.winston.server.WWS</code>. Convienence scripts are provided in bin/ to make this easier.

WWS has several command line switches that can be listed by calling it with the `--help` command line argument.

	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.server.WWS --help
	
	Usage:
	  java -jar gov.usgs.volcanoes.winston.server.WWS [--help] [-i|--noinput]
	  [<config-filename>] [-v|--verbose]
	
	I am the Winston wave server
	
	
	  [--help]
	        Prints this help message.
	
	  [-i|--noinput]
	        Do not poll keyboard for input.
	
	  [<config-filename>]
	        The config file name. (default: WWS.config)
	
	  [-v|--verbose]
	        Verbose logging.
	
	%