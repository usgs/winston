# Applications

Winston ships with several applications included in the jar file. 

## Administration
- [Admin](#admin) -- A collection of commands for administering a Winston database.
- [ImportDataless](#importdataless) -- Populate winston with station locations from a SEED dataless volume.
- [ImportHypoinverse](#importhypoinverse) -- Populate winston with station location from a Hypoinverse station file.
- [Merge](#merge) -- 
- [Upgrade](#upgrade) -- Upgrade winston schema. 

## Data Import
- [ImportEW](importew.md) -- Ingest waveforms from an Earthworm exportgenereic-style exporter.
- [ImportSAC](#importsac) -- Ingest waveforms into winston from a SAC file.
- [ImportSEED](#importseed) -- Ingest waveforms into winston from a miniSEED volulme. 
- [ImportWS](#importws) -- Import waveforms from another winston.

## Data Export
- [PlotHelicorder](#plothelicorder) -- Create a PNG helicorder plot. 
- [Winston Wave Server](WWS.md) -- Winston wave server. 

## Miscellanea
- [WinstonTools](#winstontools) -- A GUI to launch winston accessories.

---

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

### ImportDataless
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.in.metadata.ImportDataless
	Usage: ImportDataless [-c <winston.config>] <dataless>
	% 

### ImportHypoinverse
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.in.metadata.ImportHypoinverse 
	Usage: ImportDataless [-c <winston.config>] <dataless>
	% 

### ImportSac

### ImportSEED

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

### Merge
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.db.Merge 
	usage: java gov.usgs.winston.db.Merge [srcURL] [destURL] [table] [date]
	[table] is case sensitive; example: CRP_SHZ_AK
	[date] is in YYYY_MM_DD form; example: 2005_03_27
	localhost [6:19pm] % 
	
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

### Upgrade
	% java -cp lib/winston.jar gov.usgs.volcanoes.winston.db.Upgrade
	2015-12-09 06:23:45  INFO - Connected to database.
	Current Winston schema version: 1.1.1
	
	Available upgrade:
	Winston schema up-to-date, no upgrades available.
	
	Run with '--upgrade' option to perform an upgrade.
	% 

### WinstonTools
