package gov.usgs.winston.db;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;

import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to recalculate stored RSAM values
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class Deriver
{
	private static final String DEFAULT_CONFIG_FILENAME = "Deriver.config";
	private static final double DEFAULT_CHUNK_SIZE = 600.0;
	
	private static final boolean DEFAULT_RSAM_ENABLE = true;
	private static final int DEFAULT_RSAM_DELTA = 10;
	private static final int DEFAULT_RSAM_DURATION = 60;
	
	private WinstonDatabase winston;
	private InputEW input;
	private Data data;
	
	private double startTime;
	private double endTime;
	
	private Logger logger;
	
	private double chunkSize = 3600;
	
	private boolean quit = false;
	
	private boolean rsamEnable = DEFAULT_RSAM_ENABLE;
	private int rsamDelta = DEFAULT_RSAM_DELTA;
	private int rsamDuration = DEFAULT_RSAM_DURATION;
	
	private ConfigFile config;
	
	private List<String> sourceChannels;
	
	public Deriver()
	{
		logger = Log.getLogger("gov.usgs.winston");
		logger.setLevel(Level.FINE);
		config = new ConfigFile(DEFAULT_CONFIG_FILENAME);
		processConfigFile();
		input = new InputEW(winston);
		data = new Data(winston);
		deriveAll();
	}
	
	public void processConfigFile()
	{
		winston = WinstonDatabase.processWinstonConfigFile(config);
		
		sourceChannels = config.getList("channel");
		
		String timeRange = config.getString("timeRange");
		double[] t;
		try {
			t = Time.parseTimeRange(timeRange);
			startTime = t[0];
			endTime = t[1];
		} catch (ParseException e) {
			System.err.println("Can't parse timeRange");
			System.exit(1);
		}
		
		chunkSize = Util.stringToDouble(config.getString("chunkSize"), DEFAULT_CHUNK_SIZE);

		rsamEnable = Util.stringToBoolean(config.getString("rsam.enable"), DEFAULT_RSAM_ENABLE);
		rsamDelta = Util.stringToInt(config.getString("rsam.delta"), DEFAULT_RSAM_DELTA);
		rsamDuration = Util.stringToInt(config.getString("rsam.duration"), DEFAULT_RSAM_DURATION);
	}
	
	public void deriveAll()
	{
		for (String channel : sourceChannels)
		{
			logger.info("Working on " + channel);
			derive(channel, startTime, endTime);
		}
	}
	
	private void derive(String ch, double t1, double t2)
	{
		input.setRowParameters((int)chunkSize + 65, 60);
		
		double ct = t1 - chunkSize;
		List<TraceBuf> tbs = null;
		while (ct < t2)
		{
			if (quit)
				break;
			ct += chunkSize;
			
			double ret = Math.min(ct + chunkSize, t2);
			CodeTimer netTimer = new CodeTimer("net");
			try{
			tbs = data.getTraceBufs(ch, ct, ret, 0);
			} catch (UtilException e){
			}
			netTimer.stop();
			
			if (tbs != null && tbs.size() > 0)
			{
				CodeTimer inputTimer = new CodeTimer("input");
				input.rederive(tbs, rsamEnable, rsamDelta, rsamDuration);
				inputTimer.stop();
				
				logger.info(String.format("Derived %d TraceBufs in %.3fms, insert completed in %.3fms", 
						tbs.size(), netTimer.getRunTimeMillis(), inputTimer.getRunTimeMillis()));
			}
		}
	}
	
	public static void main(String[] args)
	{
		new Deriver();
	}
}
