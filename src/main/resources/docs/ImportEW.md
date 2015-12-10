# ImportEW

ImportEW imports waveforms into winston from an Earthworm export. 

--- 

## Configuration

---

## Launching
Start ImportEW with a command similar to <code>java -cp lib/winston.jar gov.usgs.volcanoes.winston.in.ew.ImportEW</code>. Convienence scripts are provided in bin/ to make this easier.

ImportEW has several command line switches that can be listed by calling ImportEW with `--help`.
	
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