package gov.usgs.winston.in.ew;

import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.MenuItem;
import gov.usgs.earthworm.WaveServer;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.Data;
import gov.usgs.winston.db.WinstonDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

/**
 *
 * @author Dan Cervelli
 */
public class ImportWS
{
	private static final String DEFAULT_CONFIG_FILENAME = "ImportWS.config";
	private static final double DEFAULT_CHUNK_SIZE = 600.0;
	private static final int DEFAULT_CHUNK_DELAY = 500;
	
	private static final boolean DEFAULT_RSAM_ENABLE = true;
	private static final int DEFAULT_RSAM_DELTA = 10;
	private static final int DEFAULT_RSAM_DURATION = 60;
	
	private WaveServer waveServer;
	private WinstonDatabase winston;
	
	private Channels channels;
	private Data data;
	private Menu menu;
	
	private ConfigFile config;
	
	private boolean createChannels;
	private boolean createDatabase;
	
	private List<ImportWSJob> jobs;
	private ImportWSJob currentJob;
	private List<String> sourceChannels;
	
	private Logger logger;
	
	private double startTime;
	private double endTime;
	
	private double chunkSize;
	private int chunkDelay;
	
	private boolean rsamEnable;
	private int rsamDelta;
	private int rsamDuration;
	
	private int totalInserted = 0;
	private double totalDownloadTime = 0;
	private double totalInsertTime = 0;
	
	private boolean requestSCNL = false;
	
	private boolean quit = false;
	
	private CodeTimer appTimer;
	
	// JSAP related stuff.
	public static String JSAP_PROGRAM_NAME = "java gov.usgs.winston.in.ew.ImportWS";
	public static String JSAP_EXPLANATION_PREFACE =
			"Winston ImportWS\n" +
			"\n" +
			"This program gets data from a Winston wave server and imports\n" +
			"it into a Winston database. See 'ImportWS.config' for more options.\n" +
			"\n";
	
	private static final String DEFAULT_JSAP_EXPLANATION =
			"All output goes to standard error.\n" +
			"The command line takes precedence over the config file.\n";
	
	private static final Parameter[] DEFAULT_JSAP_PARAMETERS = new Parameter[] {
		new FlaggedOption("timeRange", JSAP.STRING_PARSER, JSAP.NO_DEFAULT,
				JSAP.NOT_REQUIRED, 't', "timerange",
				"The time range. Relative times are assumed to be in the past.\n"),
		new FlaggedOption("waveServer", JSAP.STRING_PARSER, JSAP.NO_DEFAULT,
				JSAP.NOT_REQUIRED, 'w', "waveserver",
				"The Winston wave server to poll.\n").setUsageName("host:port"),
		new Switch("noInput", 'i', "noinput", "Do not poll keyboard for input."),
		new Switch("SCNL", 'l', "SCNL", "Always request SCNL"),
		new UnflaggedOption("configFilename", JSAP.STRING_PARSER,
				DEFAULT_CONFIG_FILENAME, JSAP.REQUIRED, JSAP.NOT_GREEDY,
				"The config file name.")
	};

	public ImportWS()
	{
		appTimer = new CodeTimer("application");
		logger = Log.getLogger("gov.usgs.winston");
		logger.setLevel(Level.FINE);
	}
	
	public ImportWS(String fileName)
	{
		this();
		config = new ConfigFile(fileName);
		processConfig();
	}

	public void setConfig(ConfigFile config)
	{
		this.config = config;
	}
	
	public void setRequestSCNL(boolean requestSCNL) {
	    this.requestSCNL = requestSCNL;
	}
	
	public boolean getRequestSCNL() {
	    return requestSCNL;
	}
	
	public void processConfig()
	{
		createDatabase = Util.stringToBoolean(config.getString("createDatabase"));
		logger.fine("createDatabase: " + createDatabase);
		
		winston = WinstonDatabase.processWinstonConfigFile(config);
		logger.fine("winston.driver: " + winston.dbDriver);
		logger.fine("winston.url: " + winston.dbURL);
		logger.fine("winston.prefix: " + winston.databasePrefix);
		logger.fine("winston.statementCacheCap: " + winston.cacheCap);
		
		if (createDatabase)
		{
			winston.checkDatabase();
		}
		
		String s = config.getString("waveServer");
		if (s == null)
			throw new RuntimeException("no waveServer string");
		
		waveServer = new WaveServer(config.getString("waveServer"));
		logger.fine("waveServer: " + waveServer.host + ":" + waveServer.port);
		
		createChannels = Util.stringToBoolean(config.getString("createChannels"));
		logger.fine("createChannels: " + createChannels);
		
		sourceChannels = config.getList("channel");
		logger.fine("sourceChannels: " + sourceChannels);
		
		String timeRange = config.getString("timeRange");
		logger.fine("timeRange: " + timeRange);
		parseTimeRange(timeRange);
		
		chunkSize = Util.stringToDouble(config.getString("chunkSize"), DEFAULT_CHUNK_SIZE);
		logger.fine("chunkSize: " + chunkSize);
		
		chunkDelay = Util.stringToInt(config.getString("chunkDelay"), DEFAULT_CHUNK_DELAY);
		logger.fine("chunkDelay: " + chunkDelay);
	
		rsamEnable = Util.stringToBoolean(config.getString("rsam.enable"), DEFAULT_RSAM_ENABLE);
		logger.fine("rsamEnable: " + rsamEnable);
		
		rsamDelta = Util.stringToInt(config.getString("rsam.delta"), DEFAULT_RSAM_DELTA);
		logger.fine("rsamDelta: " + rsamDelta);
		
		rsamDuration = Util.stringToInt(config.getString("rsam.duration"), DEFAULT_RSAM_DURATION);
		logger.fine("rsamDuration: " + rsamDuration);
		// TODO: log level
	}
	
	public void addStats(int t, double td, double ti)
	{
		totalInserted += t;
		totalDownloadTime += td;
		totalInsertTime += ti;
	}
	
	private void parseTimeRange(String timeRange)
	{		
		try
		{
			double[] tr = Time.parseTimeRange(timeRange);
			startTime = tr[0];
			endTime = tr[1];
		}
		catch (Exception e)
		{
			logger.severe("Error parsing time range: " + e.getMessage());
			System.exit(-1);
		}
		
		logger.fine(String.format("Requested time range: [%s -> %s, %s]",
				Time.toDateString(startTime),
				Time.toDateString(endTime), 
				Util.timeDifferenceToString(endTime - startTime)));
	}

	public void setWinston(WinstonDatabase w)
	{
		winston = w;
	}
	
	public void setWaveServer(WaveServer ws)
	{
		waveServer = ws;
	}
	
	private void getChannels()
	{
		channels = new Channels(winston);
		data = new Data(winston);
		waveServer.connect();
		menu = waveServer.getMenuSCNL();
		waveServer.close();
	}

	public Menu getMenu()
	{
		return menu;
	}
	
	public void createJobs()
	{
		getChannels();
		jobs = new ArrayList<ImportWSJob>();
		
		List<MenuItem> items = menu.getItems();
		for (MenuItem item : items)
		{
			for (String channel : sourceChannels)
			{
				String[] ss = channel.split("[\\$\\_ ]");
				String loc = null;
				if (ss.length == 4)
					loc = ss[3];
				
				if (item.match(ss[0], ss[1], ss[2], loc))
				{
					String wc = item.getSCNSCNL("$");
					
					if (!createChannels && !channels.channelExists(wc))
						continue;
					
					logger.fine("Remote channel matched: " + wc);
					ImportWSJob job = new ImportWSJob(winston, waveServer, this);
					job.setChannel(wc);
					job.setChunkDelay(chunkDelay);
					job.setChunkSize(chunkSize);
					job.setRSAMParameters(rsamEnable, rsamDelta, rsamDuration);
					jobs.add(job);
				}
			}
		}
	}
	
	public void startImport()
	{
		for (ImportWSJob job : jobs)
		{
			currentJob = job;
			logger.info(job.getChannel() + ": finding gaps");
			List<double[]> gaps = data.findGaps(job.getChannel(), startTime, endTime);
			for (double[] gap : gaps)
				job.addSpan(gap[0], gap[1]);

			job.go();
			
			if (quit)
				break;
		}
		appTimer.stop();
		logger.info(String.format("%d tbs inserted, total download time: %s, total insert time: %s (%.3fms/tb)", 
				totalInserted, Util.timeDifferenceToString(totalDownloadTime / 1000), Util.timeDifferenceToString(totalInsertTime / 1000),
				totalInsertTime / (double)totalInserted));
		logger.info("Total run time: " + Util.timeDifferenceToString(appTimer.getRunTimeMillis() / 1000.0));
		quit = true;
	}
	
	public void go()
	{
		Thread launchThread = new Thread(new Runnable()
				{
					public void run()
					{
						startImport();
					}
				});
		launchThread.start();
	}

	public void quit()
	{
		if (currentJob != null)
			currentJob.quit();
		else
			logger.info("Null job");
		quit = true;
		logger.info("Quitting cleanly.");
	}
	
	/**
	 * Find and parse the command line arguments.
	 *
	 * @param args The command line arguments.
	 */
	public static JSAPResult getArguments(String[] args) {
		JSAPResult config = null;
		try
		{
			SimpleJSAP jsap = new SimpleJSAP(
					JSAP_PROGRAM_NAME,
					JSAP_EXPLANATION_PREFACE + DEFAULT_JSAP_EXPLANATION,
					DEFAULT_JSAP_PARAMETERS
					);
			
			config = jsap.parse(args);
			
			if (jsap.messagePrinted())
			{
				// The following error message is useful for catching the case
				// when args are missing, but help isn't printed.
				if (!config.getBoolean("help"))
					System.err.println("Try using the --help flag.");

				System.exit(1);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
		return config;
	}

	public Logger getLogger() {
		return logger;
	}
	
	public static void main(String[] args) throws IOException
	{
		JSAPResult config = getArguments(args);
		ImportWS w = new ImportWS(config.getString("configFilename"));

		if (config.getString("timeRange") != null)
			w.parseTimeRange(config.getString("timeRange"));		

		if (config.getString("waveServer") != null)
			w.waveServer = new WaveServer(config.getString("waveServer"));		
		
		boolean acceptCommands = !(config.getBoolean("noInput"));

		w.setRequestSCNL((config.getBoolean("SCNL")));
		
		w.createJobs();
		w.go();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				
		while (acceptCommands && !w.quit)
		{
			String s = in.readLine();
			if (s != null)
			{
				s = s.toLowerCase().trim();
				if (s.equals("q"))
					w.quit();
			}
		}
	}

	public void setQuit(boolean b) {
		quit = b;		
	}

}
