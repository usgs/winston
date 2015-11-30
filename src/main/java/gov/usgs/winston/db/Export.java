package gov.usgs.winston.db;

import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class Export
{
	private WinstonDatabase winston;
	private Data data;

	private static String dbDriver;
	private static String dbURL;
	private static String dbPrefix;
	
	public Export(WinstonDatabase w)
	{
		winston = w;
		data = new Data(winston);
	}

	public static void readConfigFile()
	{
		ConfigFile config = new ConfigFile("Winston.config");
		dbDriver = config.getString("winston.driver");
		dbURL = config.getString("winston.url");		
		dbPrefix = config.getString("winston.prefix");
	}
	
	public void export(String code, String pre, double t1, double t2)
	{
		try
		{
			double maxSize = 3600;
			double nst = t1 - (t1 % 5);
//			double net = t2 + (5 - t2 % 5);
			double ct = nst;
			int cnt = 0;
			while (ct < t2)
			{
				Wave sw;
				if (t2 - ct > maxSize)
				{
					sw = data.getWave(code, ct, ct + maxSize, 0);
					ct += maxSize;
				}
				else
				{
					sw = data.getWave(code, ct, ct + maxSize, 0);
					ct = t2;
				}
				if (sw != null) {
				    SeismicDataFile file = SeismicDataFile.getFile(pre + "_" + code + "_" + cnt + ".txt");
				    file.putWave(code, sw);
				    file.write();
				}
				System.out.println((ct - t1) + "s");
				cnt++;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws ParseException
	{
		if (args.length != 4)
		{
			System.out.println("Usage: java gov.usgs.winston.db.Export [code] [prefix] [yyyymmddhhmmss] [yyyymmddhhmmss]");
			System.out.println();
			System.out.println("Database parameters ('winston.url', 'winston.driver', 'winston.prefix')\n" +
					"must be in 'Winston.config'.");
			System.out.println();
			System.out.println("Input time is in GMT.");
			System.out.println();
			System.out.println("[code] is $ separated, example: CRP$EHZ$AV");
			System.out.println();
			System.out.println("Output files will have names like: '[prefix]_[code]_[number].txt'.");
			System.exit(1);
		}
	
		readConfigFile();
		String code = args[0];
		String prefix = args[1];
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		double t1 = Util.dateToJ2K(dateFormat.parse(args[2]));
		double t2 = Util.dateToJ2K(dateFormat.parse(args[3]));
		System.out.println("Attempting to extract " + (t2 - t1) + " seconds from " + code);
		WinstonDatabase winston = new WinstonDatabase(dbDriver, dbURL, dbPrefix);
		Export export = new Export(winston);
		export.export(code, prefix, t1, t2);
	}
}
