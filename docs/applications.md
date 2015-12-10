# Applications

Winston ships with several applicaions included in the jar file. 

## Administration
- [Admin](#Admin) -- A collection of commands for administering a Winston database.
- [ImportDataless](#ImportDataless) -- Populate winston with station locations from a SEED dataless volume.
- [ImportHypoinverse](#ImportHypoinverse) -- Populate winston with station location from a Hypoinverse station file.
- [Merge](#Merge) -- 
- [Upgrade](#Upgrade) -- Upgrade winston schema. 

## Data Import
- [ImportEW](#ImportEW) -- Ingest waveforms from an Earthworm exportgenereic-style exporter.
- [ImportSAC](#ImportSAC) -- Ingest waveforms into winston from a SAC file.
- [ImportSEED](#ImportSEED) -- Ingest waveforms into winston from a miniSEED volulme. 
- [ImportWS](#ImportWS) -- Import waveforms from another winston.

## Data Export
- [PlotHelicorder](#PlotHelicorder) -- Create a PNG helicorder plot. 
- [WWS](#WWS) -- Winston wave server. 

## Miscellanea
- [WinstonTools](#WinstonTools) -- A GUI to launch winston accessories.

---

<a name="Admin"></a>
### Admin


	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.db.Admin --help
	Winston Admin

	A collection of commands for administering a Winston database.
	Information about connecting to the Winston database must be present
	in Winston.config in the current directory.
	
	Usage:
	  java gov.usgs.winston.db.Admin [options] command [command arguments]
	
	Valid options:
	  --delay seconds                 the delay between each channel for commands
	                                  for multiple channels
	
	Valid commands:
	  --list                          lists all channels
	  --list times                    lists all channels with time span
	  --delete channel                delete the specified channel
	  --deletex SSSS$CCC$NN[$LL]      delete the specified channels where:
	                                  SSSS is the station,
	                                  CCC is the channel which may contain
	                                  a wild card (%),
	                                  SSSS is the station,
	                                  NN is the network,
	                                  LL is the optional location which may contain
	                                  a wild card (%)
	  --span                          recalculate table spans
	  --purge channel days            purge the specified channel for the
	                                  specified number of days
	  --purgex channel days           purge the specified channel for the
	                                  specified number of days where the channel
	                                  may contain a wild card (%) anywhere
	  --repair YYYY_MM_DD [channel]   repair all tables on given day
	                                  optionally, just repair the specified channel
	
	% 
	
<a name="ImportDataless"></a>
### ImportDataless
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.in.metadata.ImportDataless
	Usage: ImportDataless [-c <winston.config>] <dataless>
	% 

<a name="ImportEW"></a>
### ImportEW
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.in.ew.ImportEW --help
	
	Usage:
	  java gov.usgs.winston.in.ew.ImportEW [--help] [(-l|--log-level) <logLevel>]
	  [-0|--logoff] [-1|--lognormal] [-2|--loghigh] [-3|--logall] [-i|--noinput]
	  [<configFilename>]
	
	Winston ImportEW
	
	This program gets data from an Earthworm export process and imports
	it into a Winston database. See 'ImportEW.config' for more options.
	
	All output goes to both standard error and the file log.
	
	While the process is running (and accepting console input) you can enter
	these commands into the console (followed by [Enter]):
	0: turn logging off.
	1: normal logging level (WARNING).
	2: high logging level (FINE).
	3: log everything.
	s: display status information.
	c[col][-]: channel list, sorted by col, - for descending. Examples: c, cl-, cx
	i: no longer accept console input.
	q: quit cleanly.
	ctrl-c: quit now.
	
	Note that if console input is disabled the only way to
	terminate the program is with ctrl-c or by killing the process.
	
	
	  [--help]
	        Prints this help message.
	
	  [(-l|--log-level) <logLevel>]
	        The level of logging to start with
	        This may consist of either a java.util.logging.Level name or an integer
	        value.
	        For example: "SEVERE", or "1000"
	
	  [-0|--logoff]
	        Turn logging off (equivalent to --log-level OFF).
	
	  [-1|--lognormal]
	        Normal (default) logging level (equivalent to --log-level WARNING).
	
	  [-2|--loghigh]
	        High logging level (equivalent to --log-level FINE).
	
	  [-3|--logall]
	        High logging level (equivalent to --log-level ALL).
	
	  [-i|--noinput]
	        Don't accept input from the console.
	
	  [<configFilename>]
	        The config file name.
	
	% 

<a name="ImportHypoinverse"></a>
### ImportHypoinverse
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.in.metadata.ImportHypoinverse 
	Usage: ImportDataless [-c <winston.config>] <dataless>
	% 
	
<a name="ImportSac"></a>
### ImportSac

<a name="ImportSEED"></a>
### ImportSEED

<a name="ImportWS"></a>
### ImportWS
	java -cp lib/winston.jar gov.usgs.volcanoes.winston.in.ew.ImportWS --help
	
	Usage:
	  java gov.usgs.winston.in.ew.ImportWS [--help] [(-t|--timerange) <timeRange>]
	  [(-w|--waveserver) <host:port>] [-i|--noinput] [-l|--SCNL] <configFilename>
	
	Winston ImportWS
	
	This program gets data from a Winston wave server and imports
	it into a Winston database. See 'ImportWS.config' for more options.
	
	All output goes to standard error.
	The command line takes precedence over the config file.
	
	
	  [--help]
	        Prints this help message.
	
	  [(-t|--timerange) <timeRange>]
	        The time range. Relative times are assumed to be in the past.
	
	  [(-w|--waveserver) <host:port>]
	        The Winston wave server to poll.
	
	  [-i|--noinput]
	        Do not poll keyboard for input.
	
	  [-l|--SCNL]
	        Always request SCNL
	
	  <configFilename>
	        The config file name. (default: ImportWS.config)
	
	% 
	
<a name="Merge"></a>
### Merge
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.db.Merge 
	usage: java gov.usgs.winston.db.Merge [srcURL] [destURL] [table] [date]
	[table] is case sensitive; example: CRP_SHZ_AK
	[date] is in YYYY_MM_DD form; example: 2005_03_27
	localhost [6:19pm] % 
	
<a name="PlotHelicorder"></a>
### PlotHelicorder
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.PlotHelicorder 
	Server/station/time options
	-wws  [host]:[port], the WWS, mandatory argument
	-s    station, mandatory argument
	-e    end time [now], format: 'YYYYMMDDHHMMSS' (GMT) or 'now'
	-m    minutes on x-axis [20]
	-h    hours on y-axis [12]
	-tz   time zone abbreviation [GMT]
	-to   time zone offset, hours [0]
	
	Output options
	-x    total plot x-pixel size [1000]
	-y    total plot y-pixel size [1000]
	-lm   left margin pixels [70]
	-rm   right margin pixels [70]
	-o    output file name [heli.png]
	-c    clip value, a number [auto]
	-b    bar range, a number [auto]
	-r    show clipped trace as red, 0 or 1 [0]
	% 

<a name="Upgrade"></a>
### Upgrade
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.db.Upgrade
	2015-12-09 06:23:45  INFO - Connected to database.
	Current Winston schema version: 1.1.1
	
	Available upgrade:
	Winston schema up-to-date, no upgrades available.
	
	Run with '--upgrade' option to perform an upgrade.
	% 

<a name="WinstonTools"></a>
### WinstonTools

<a name="WWS"</a>
### WWS
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