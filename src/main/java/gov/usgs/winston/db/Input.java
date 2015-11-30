package gov.usgs.winston.db;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * A class for inputting data into the Winston database.
 *
 * TODO: helicorder seconds spanning tables.
 * TODO: bulk-insert method.
 * TODO: clean up. 
 *  
 *
 * @author Dan Cervelli
 */
public class Input
{
	public enum InputResult { ERROR, SUCCESS, SUCCESS_CREATED_TABLE };
	
	private WinstonDatabase winston;
	private DateFormat dateFormat;
	private int maxDays = 0;

	private HashMap<String, ChannelInputOptimizer> channelOptimizers;
	private HashSet<String> checkTableCache;

	private Data data;

	public Input(WinstonDatabase w)
	{
		winston = w;
		data = new Data(w);
		channelOptimizers = new HashMap<String, ChannelInputOptimizer>();
		checkTableCache = new HashSet<String>();
		dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public void setWinston(WinstonDatabase db)
	{
		winston = db;
	}
	
	/**
	 * Sets the maximum number of days (tables) allowed after a TraceBuf is 
	 * put into the table.  WARNING: if the value is >0 then Import WILL drop
	 * tables, data will be lossed. 
	 * @param i the maximum number of days
	 */
	public void setMaxDays(int i)
	{
		if (i >= 0)
			maxDays = i;
	}

	public void setTimeSpan(String code, double st, double et)
	{
		if (!winston.checkConnect())
			return;
		try
		{
			ChannelInputOptimizer opt = (ChannelInputOptimizer)channelOptimizers.get(code);

			if (!Double.isNaN(st))
			{
				if (opt != null)
					opt.t1 = st;
				winston.getStatement().execute("UPDATE `" + winston.databasePrefix + "_ROOT`.channels SET st=" + st + " WHERE code='" + code + "'");
			}
			if (!Double.isNaN(et))
			{
				if (opt != null)
					opt.t2 = et;	
				winston.getStatement().execute("UPDATE `" + winston.databasePrefix + "_ROOT`.channels SET et=" + et + " WHERE code='" + code + "'");
			}
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Could not set time span for channel: " + code, e);
		}
	}

	/*
	protected void purgeTables(String code)
	{
		if (maxDays <= 0)
			return;

		List list = getDayTables(code);
		if (list.size() > maxDays)
		{
			int numToDelete = list.size() - maxDays;
			for (int i = 0; i < numToDelete; i++)
			{
				String table = (String)list.get(i);
				try
				{
					if (!winston.useDatabase(code))
						return;
					winston.getStatement().execute("DROP TABLE " + table);
					String ss[] = table.split("\\$\\$");
					winston.getStatement().execute("DROP TABLE " + ss[0] + "$$H" + ss[1]);
				}
				catch (Exception e)
				{
					winston.getLogger().severe("Could not drop old table: " + code + ".  Are permissions set properly?");
				}
			}
			try
			{
				String nextLowestTable = (String)list.get(numToDelete);
				ResultSet rs = winston.getStatement().executeQuery("SELECT MIN(st) FROM " + nextLowestTable);
				rs.next();
				double t1 = rs.getDouble(1);
				setTimeSpan(code, t1, Double.NaN);
				rs.close();
			}
			catch (Exception e)
			{
				winston.getLogger().severe("Could not update span after dropping table: " + code);
			}
		}
	}
	*/
	
	/**
	 * Purge tables.
	 * @param channel the channel
	 * @param days the number of days
	 * @return the Admin or null if none.
	 */
	public Admin purgeTables(String channel, int days)
	{
		return purgeTables(channel, days, (Admin)null);
	}
	
	/**
	 * Purge tables.
	 * @param channel the channel
	 * @param days the number of days
	 * @param admin the Admin or null to create one if needed.
	 * @return the Admin or null if none.
	 */
	public Admin purgeTables(String channel, int days, Admin admin)
	{
		if (days <= 0)
			return admin;

		SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date now = CurrentTime.getInstance().nowDate();
		Date then = new Date(now.getTime() - ((long)days * 86400000L));
		
		winston.getLogger().info("Purging '" + channel + "' tables before: " + df.format(then));

		List<String> list = getDayTables(channel);
		if (list == null || list.size() == 0)
		{
			winston.getLogger().info("No day tables found");
			return admin;
		}

		boolean deleted = false;
		boolean setTime = false;
		for (String table : list)
		{
			String ss[] = table.split("\\$\\$");
			
			if (df.format(then).compareTo(ss[1]) > 0)
			{
				try
				{
					if (!winston.useDatabase(channel))
						return admin;
					
					checkTableCache.remove(table);
					
					winston.getStatement().execute("DROP TABLE `" + table + "`");
					winston.getStatement().execute("DROP TABLE `" + ss[0] + "$$H" + ss[1] + "`");
					deleted = true;
					winston.getLogger().info("Deleted table: " + table);
				}
				catch (Exception e)
				{
					winston.getLogger().severe("Could not drop old table: " + channel + ".  Are permissions set properly?");
				}
			}
			else
			{
				if (deleted)
				{
					try
					{
						String nextLowestTable = table;
						ResultSet rs = winston.getStatement().executeQuery("SELECT MIN(st) FROM `" + nextLowestTable + "`");
						rs.next();
						double t1 = rs.getDouble(1);
						setTimeSpan(channel, t1, Double.NaN);
						rs.close();
						setTime = true;
					}
					catch (Exception e)
					{
						winston.getLogger().severe("Could not update span after dropping table: " + channel);
					}
				}
				break;
			}
		}
		if (deleted && !setTime)
		{
			// must have deleted all of the tables, just delete the channel entirely
			winston.getLogger().info("Permanently deleting channel: " + channel);
			if (admin == null)
				admin = new Admin(winston);
			admin.deleteChannel(channel);
		}
		return admin;
	}

	private boolean createDayTable(String code, String date)
	{
		try
		{
			winston.getStatement().execute("CREATE TABLE `" + code + "$$" + date + "` (" + "st DOUBLE PRIMARY KEY, " + "et DOUBLE, " + "sr DOUBLE, " + "datatype CHAR(3), " + "tracebuf BLOB) " + winston.tableEngine);

			winston.getStatement().execute("CREATE TABLE `" + code + "$$H" + date + "` (" + "j2ksec DOUBLE PRIMARY KEY, " + "smin INT, " + "smax INT, " + "rcnt INT, " + "rsam DOUBLE) " + winston.tableEngine);

			purgeTables(code, maxDays);
			return true;
		}
		catch (Exception ex)
		{
			winston.getLogger().severe("Could not create day table: " + code + "$" + date + ".  Are permissions set properly?");
		}
		return false;
	}

	/**
	 * This is private because it is only meant to be called from inputTraceBuf.
	 * It is expected that the correct database is currently in use.
	 * 
	 * This function checks to see if a data table exists, if it does not, it creates
	 * one.
	 * 
	 * TODO: revisit; this doesn't seem clean.
	 * 
	 * @param table the table name to check 
	 */
	private boolean checkTable(String code, String date)
	{
		String table = code + "$$" + date;
		if (checkTableCache.contains(table))
			return true;

		try
		{
			ResultSet rs = winston.getStatement().executeQuery("SELECT COUNT(*) FROM `" + table + "`");
			boolean result = rs.next();
			if (result)
				checkTableCache.add(table);
			rs.close();
			return result;
		}
		catch (Exception e)
		{
			checkTableCache.clear();
			return createDayTable(code, date);
		}
	}

	private boolean tableExists(String code, String date)
	{
		String table = code + "$$" + date;
		if (checkTableCache.contains(table))
			return true;

		try
		{
			ResultSet rs = winston.getStatement().executeQuery("SELECT COUNT(*) FROM `" + table + "`");
			boolean result = rs.next();
			if (result)
				checkTableCache.add(table);
			rs.close();
			return result;
		}
		catch (Exception e)
		{}
//			checkTableCache.clear();
//			return createDayTable(code, date);
		return false;
	}
	
	private boolean createTable(String code, String date)
	{
		try
		{
			winston.getStatement().execute("CREATE TABLE `" + code + "$$" + date + "` (" + "st DOUBLE PRIMARY KEY, " + "et DOUBLE, " + "sr DOUBLE, " + "datatype CHAR(3), " + "tracebuf BLOB) " + winston.tableEngine);
			winston.getStatement().execute("CREATE TABLE `" + code + "$$H" + date + "` (" + "j2ksec DOUBLE PRIMARY KEY, " + "smin INT, " + "smax INT, " + "rcnt INT, " + "rsam DOUBLE) " + winston.tableEngine);
//			purgeTables(code, maxDays);
			return true;
		}
		catch (Exception ex)
		{
			winston.getLogger().severe("Could not create day table: " + code + "$" + date + ".  Are permissions set properly?");
		}
		return false;
	}
	
	public List<String> getDayTables(String code)
	{
		try
		{
			ArrayList<String> list = new ArrayList<String>(10);
			if (!winston.useDatabase(code))
				return null;
			ResultSet rs = winston.getStatement().executeQuery("SHOW TABLES");
			while (rs.next())
				list.add(rs.getString(1));
			rs.close();
			Collections.sort(list);
			ArrayList<String> dayList = new ArrayList<String>(list.size() / 2);
			for (int i = 0; i < list.size(); i++)
			{
				String table = (String)list.get(i);
				String day = table.substring(table.indexOf("$$") + 2);
				if (day.length() == 10 && day.charAt(4) == '_' && day.charAt(7) == '_' && Character.isDigit(day.charAt(0)) && Character.isDigit(day.charAt(9)))
				{
					dayList.add(table);
				}
			}
			return dayList;
		}
		catch (Exception e)
		{
			winston.getLogger().severe("Could not generate list of tables: " + code);
		}
		return null;
	}

	public void calculateSpan(String code)
	{
		try
		{
			List<String> dayList = getDayTables(code);
			double mint = 1E300;
			double maxt = -1E300;
			String table = (String)dayList.get(0);
			ResultSet rs = winston.getStatement().executeQuery("SELECT MIN(st) FROM `" + table + "`");
			rs.next();
			mint = Math.min(mint, rs.getDouble(1));
			table = (String)dayList.get(dayList.size() - 1);
			rs = winston.getStatement().executeQuery("SELECT MAX(et) FROM `" + table + "`");
			rs.next();
			maxt = Math.max(maxt, rs.getDouble(1));
			setTimeSpan(code, mint, maxt);
			rs.close();
		}
		catch (Exception e)
		{
			winston.getLogger().severe("Could not calculate span: " + code);
		}
	}

	class ChannelInputOptimizer
	{
		String code;
		double t1;
		double t2;
		TreeMap<Double, double[]> data;

		public ChannelInputOptimizer(String c)
		{
			code = c;
			data = new TreeMap<Double, double[]>();
			t1 = Double.NaN;
			t2 = Double.NaN;
		}

		public boolean isInitialized()
		{
			return !Double.isNaN(t1);
		}

		// TODO: optimize.  garbage generator.
		public void putData(double j2k, double[] d)
		{
			data.put(j2k, d);
			if (data.size() > 60)
			{
				int numToDelete = data.size() - 30;
				Iterator<Double> it = data.keySet().iterator();
				for (int i = 0; i < numToDelete; i++)
				{
					it.next();
					it.remove();
				}
			}
		}

		public double[] getData(double j2k)
		{
			return data.get(j2k);
		}
	}

	private ArrayList<String> locks = new ArrayList<String>(9);
	private boolean writeLocks = false;
	private ChannelInputOptimizer currentLock;

	public void setWriteLock(boolean b)
	{
		writeLocks = b;
	}

	public void unlockTables()
	{
		if (currentLock == null)
			return;
		try
		{
			winston.getStatement().execute("UNLOCK TABLES");
			setTimeSpan(currentLock.code, currentLock.t1, currentLock.t2);
			currentLock = null;
			writeLocks = false;
			locks.clear();
		}
		catch (SQLException e)
		{
			winston.getLogger().log(Level.SEVERE, "Exception while unlocking tables.", e);
		}
	}

	/**
	 * This one is used by ImportEW.
	 * 
	 * @param tb
	 * @return result
	 */
	public InputResult inputTraceBuf(TraceBuf tb)
	{
		InputResult result = InputResult.ERROR;
		try
		{
			String code = tb.toWinstonString();
			ChannelInputOptimizer opt = (ChannelInputOptimizer)channelOptimizers.get(code);
			if (opt == null)
			{
				opt = new ChannelInputOptimizer(code);
				channelOptimizers.put(code, opt);
				double[] span = data.getTimeSpan(code);
				opt.t1 = span[0];
				opt.t2 = span[1];
			}
			
			winston.useDatabase(code);
			double ts = tb.getStartTimeJ2K();
			String date = dateFormat.format(Util.j2KToDate(ts));
			boolean createdTable = false;
			if (!tableExists(code, date))
			{
				if (createTable(code, date))
					createdTable = true;
				else
					throw new Exception("Could not create table.");
			}
			
			String table = code + "$$" + date;
			
			if (writeLocks && !locks.contains(table))
			{
				currentLock = opt;
				locks.add(table);
				locks.add(code + "$$H" + date);
				winston.getStatement().execute("LOCK TABLES " + table + " WRITE, " + code + "$$H" + date + " WRITE");
			}
			
			PreparedStatement insert = winston.getPreparedStatement("INSERT IGNORE INTO `" + table + "` VALUES (?,?,?,?,?)");
			insert.setDouble(1, ts);
			insert.setDouble(2, tb.getEndTimeJ2K());
			insert.setDouble(3, tb.samplingRate());
			insert.setString(4, tb.dataType());
			byte[] stripped = Util.resize(tb.bytes, tb.bytes.length - 1);
			byte[] compressed = Util.compress(stripped);
			insert.setBytes(5, compressed);
			insert.executeUpdate();
			
			opt.t1 = Math.min(opt.t1, tb.getStartTimeJ2K());
			opt.t2 = Math.max(opt.t2, tb.getEndTimeJ2K());
			
			if (!writeLocks)
				setTimeSpan(code, opt.t1, opt.t2);
//			setTimeSpan(code, opt.t1, opt.t2);

			///----- now insert/update helicorder/rsam data
			table = code + "$$H" + date;
			double fst = Math.floor(tb.getStartTimeJ2K());
			double cet = Math.ceil(tb.getEndTimeJ2K());
			double[][] heliList = new double[((int)Math.round(cet - fst)) + 1][];
			int j = 0;
			for (int i = (int)Math.round(fst); i <= (int)Math.round(cet); i++)
			{
				double[] d = opt.getData(i);
				if (d == null)
				{
					boolean filled = false;
					/*
					 // needed for static importer
					if (fillHeli)
					{
						ResultSet rs = winston.executeQuery("SELECT j2ksec, smin, smax, rcnt, rsam FROM " + table + " WHERE j2ksec=" + i);
						if (rs.next())
						{
							filled = true;
							d = new double[] { rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4), rs.getDouble(5) }; 
						}
					}
					*/
					
					if (!filled)
					{
						d = new double[] { i, Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 0 };
						winston.getStatement().execute("INSERT IGNORE INTO `" + table + "` VALUES (" + i + "," + Integer.MAX_VALUE + "," + Integer.MIN_VALUE + ",0,0)");
					}
				}
				heliList[j++] = d;
			}
			double st = tb.getStartTimeJ2K();
			double dt = 1 / tb.samplingRate();
			for (int i = 0; i < tb.numSamples(); i++)
			{
				j = (int)(Math.floor(st) - fst);
				double[] d = heliList[j];
				int sample = tb.samples()[i];
				d[1] = Math.min(d[1], sample);
				d[2] = Math.max(d[2], sample);
				d[4] = (d[4] * d[3] + Math.abs(sample)) / (d[3] + 1);
				d[3] = d[3] + 1;
				st += dt;
			}

			for (int i = 0; i < heliList.length; i++)
			{
				int j2k = (int)Math.round(fst + i);
				winston.getStatement().execute("UPDATE `" + table + "` SET smin=" + (int)heliList[i][1] + ", smax=" + (int)heliList[i][2] + ", rcnt=" + (int)heliList[i][3] + ", rsam=" + heliList[i][4] + " WHERE j2ksec=" + j2k);
				opt.putData(j2k, heliList[i]);
			}
			result = createdTable ? InputResult.SUCCESS_CREATED_TABLE : InputResult.SUCCESS;
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Could not insert TraceBuf: " + tb, e);
		}
		return result;
	}
	
	/**
	 * Inputs a TraceBuf into the Winston database.
	 * 
	 * It seems that this method is slow on older machines.  It's not clear that
	 * it is the fault of the compress function.  I've tried prepared 
	 * statements in some places and they seem to be marginally slower.  There
	 * probably will be a real speed increase if the optimization code could
	 * be written to avoid extra garbage generation and didn't use TreeMap.
	 * 
	 * TODO: this is still used by the static importers.  Need to eliminate.
	 * 
	 * @param tb the TraceBuf
	 */
	public boolean inputTraceBuf(TraceBuf tb, boolean fillHeli)
	{
		try
		{
			String code = tb.toWinstonString();
			ChannelInputOptimizer opt = (ChannelInputOptimizer)channelOptimizers.get(code);
			if (opt == null)
			{
				opt = new ChannelInputOptimizer(code);
				channelOptimizers.put(code, opt);
				double[] span = data.getTimeSpan(code);
				opt.t1 = span[0];
				opt.t2 = span[1];
			}
			
			winston.useDatabase(code);
			double ts = tb.getStartTimeJ2K();
			String date = dateFormat.format(Util.j2KToDate(ts));
			checkTable(code, date);
			String table = code + "$$" + date;
			if (writeLocks && !locks.contains(table))
			{
				currentLock = opt;
				locks.add(table);
				locks.add(code + "$$H" + date);
				winston.getStatement().execute("LOCK TABLES `" + table + "` WRITE, `" + code + "$$H" + date + "` WRITE");
			}

			PreparedStatement insert = winston.getPreparedStatement("INSERT INTO `" + table + "` VALUES (?,?,?,?,?)");
			insert.setDouble(1, ts);
			insert.setDouble(2, tb.getEndTimeJ2K());
			insert.setDouble(3, tb.samplingRate());
			insert.setString(4, tb.dataType());
			byte[] stripped = Util.resize(tb.bytes, tb.bytes.length - 1);
			byte[] compressed = Util.compress(stripped);
			insert.setBytes(5, compressed);
			try
			{
				insert.executeUpdate();
			}
			catch (SQLException ex)
			{
				if (ex.getMessage().startsWith("Duplicate entry"))
				{
					insert.close();
					return false;
				}
			}

			opt.t1 = Math.min(opt.t1, tb.getStartTimeJ2K());
			opt.t2 = Math.max(opt.t2, tb.getEndTimeJ2K());
			if (!writeLocks)
				setTimeSpan(code, opt.t1, opt.t2);

			///----- now insert/update helicorder/rsam data
			table = code + "$$H" + date;
			double fst = Math.floor(tb.getStartTimeJ2K());
			double cet = Math.ceil(tb.getEndTimeJ2K());
			double[][] heliList = new double[((int)Math.round(cet - fst)) + 1][];
			int j = 0;
			for (int i = (int)Math.round(fst); i <= (int)Math.round(cet); i++)
			{
				double[] d = opt.getData(i);
				if (d == null)
				{
					boolean filled = false;
					if (fillHeli)
					{
						ResultSet rs = winston.executeQuery("SELECT j2ksec, smin, smax, rcnt, rsam FROM `" + table + "` WHERE j2ksec=" + i);
						if (rs.next())
						{
							filled = true;
							d = new double[] { rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4), rs.getDouble(5) }; 
						}
					}
					
					if (!filled)
					{
						d = new double[] { i, Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 0 };
					
//					insert = winston.getPreparedStatement("INSERT IGNORE INTO " + table + " VALUES (?," + Integer.MAX_VALUE + "," + Integer.MIN_VALUE + ",0,0)");
//					insert.setInt(1, i);
//					insert.executeUpdate();
						winston.getStatement().execute("INSERT IGNORE INTO `" + table + "` VALUES (" + i + "," + Integer.MAX_VALUE + "," + Integer.MIN_VALUE + ",0,0)");
					}
				}
				heliList[j++] = d;
			}
			double st = tb.getStartTimeJ2K();
			double dt = 1 / tb.samplingRate();
			for (int i = 0; i < tb.numSamples(); i++)
			{
				j = (int)(Math.floor(st) - fst);
				double[] d = heliList[j];
				int sample = tb.samples()[i];
				d[1] = Math.min(d[1], sample);
				d[2] = Math.max(d[2], sample);
				d[4] = (d[4] * d[3] + Math.abs(sample)) / (d[3] + 1);
				d[3] = d[3] + 1;
				st += dt;
			}

			for (int i = 0; i < heliList.length; i++)
			{
				int j2k = (int)Math.round(fst + i);
//				insert = winston.getPreparedStatement("UPDATE " + table + " SET smin=?, smax=?, rcnt=?, rsam=? WHERE j2ksec=?");
//				insert.setInt(1, (int)heliList[i][1]);
//				insert.setInt(2, (int)heliList[i][2]);
//				insert.setInt(3, (int)heliList[i][3]);
//				insert.setDouble(4, heliList[i][4]);
//				insert.setInt(5, j2k);
//				insert.executeUpdate();
				winston.getStatement().execute("UPDATE `" + table + "` SET smin=" + (int)heliList[i][1] + ", smax=" + (int)heliList[i][2] + ", rcnt=" + (int)heliList[i][3] + ", rsam=" + heliList[i][4] + " WHERE j2ksec=" + j2k);

				opt.putData(j2k, heliList[i]);
			}
			return true;
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Could not insert TraceBuf: " + tb, e);
		}
		return false;
	}
}