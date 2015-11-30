package gov.usgs.winston.db;

import gov.usgs.util.ConfigFile;
import gov.usgs.winston.Channel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Dan Cervelli
 */
public class Admin
{
	/** The channel expression SQL wild card. */
	public static final String WILDCARD = "%";
	private WinstonDatabase winston;
	private Channels channels;
	private Input input;

	/**
	 * Constructor
	 * @param w WinstonDatabase
	 */
	public Admin(WinstonDatabase w)
	{
		winston = w;
		channels = new Channels(winston);
		input = new Input(winston);
	}
	
	/**
	 * Constructor
	 * @param driver
	 * @param url
	 * @param db
	 */
	public Admin(String driver, String url, String db)
	{
		this(new WinstonDatabase(driver, url, db));
	}
	
	/**
	 * Calculate table spans
	 */
	public void calculateTableSpans()
	{
		try
		{
			List<Channel> st = channels.getChannels();
			for (Iterator<Channel> it = st.iterator(); it.hasNext(); )
			{
				String code = ((Channel)it.next()).getCode();
				System.out.print(code + "...");
				input.calculateSpan(code);
				System.out.println("done.");
			}
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Error during calculateTableSpans().", e);
		}
	}
	
	/**
	 * List channels to system out
	 * @param times if true, include min & max times
	 */
	public void listChannels(boolean times)
	{
		List<Channel> st = channels.getChannels();
		for (Channel ch : st)
		{
			String code = ch.getCode();
			System.out.print(code);
			if (times)
				System.out.print("\t" + ch.getMinTime() + "\t" + ch.getMaxTime());
			System.out.println();
		}
	}
	
	/**
	 * Remove a channel from the database
	 * @param ch channel to remove
	 * @throws SQLException if a SQL exception occurs.
	 */
	private void doDeleteChannel(String ch) throws SQLException
	{
		winston.getStatement().execute("DELETE FROM channels WHERE code='" + ch + "'");
		winston.getStatement().execute("DROP DATABASE `" + winston.databasePrefix + "_" + ch + "`");
	}
	
	/**
	 * Remove a channel from the database
	 * @param ch channel to remove
	 */
	public void deleteChannel(String ch)
	{
		try
		{
			winston.useRootDatabase();
			doDeleteChannel(ch);
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Error during deleteChannel().", e);
		}
	}

	/**
	 * Remove channels from the database
	 * @param chx the channel expression
	 * @param delay the delay in milliseconds between channels
	 */
	public void deleteChannels(String chx, long delay)
	{
		String ch = chx;
		try
		{
			List<String> channelList = channels.getChannelCodes(chx);
			if (channelList == null || channelList.size() == 0)
			{
				winston.getLogger().info("deleteChannels: no channels found (" + chx + ")");
			}
			else
			{
				// delete each channel
				for (int i = 0; i < channelList.size(); i++)
				{
					ch = channelList.get(i);
					if (i != 0 && delay != 0)
						Thread.sleep(delay);
					doDeleteChannel(ch);
					winston.getLogger().info("Deleted channel: " + ch);
				}
				ch = chx;
			}
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Error during deleteChannels(" + ch + ")", e);
		}
	}

	/**
	 * Determines if the channel expression is valid. The channel
	 * expression used for <code>deleteChannels</code> is restricted
	 * and should be verified.
	 * @param chx the channel expression.
	 * @return true if valid, false otherwise.
	 * @see #deleteChannels(String, long)
	 */
	public boolean isChannelExpressionValid(String chx)
	{
		boolean valid = true;
		int index;
		// ensure the station and network do not have wild card
		String[] ca = chx.split("\\$");
		switch (ca.length)
		{
		case 3: // station$channel$network
			if (ca[0].indexOf(WILDCARD) >= 0
					|| ((index = ca[2].indexOf(WILDCARD)) >= 0 && index <= 1))
			{
				valid = true;
			}
			break;
		case 4: // station$channel$network$location
			if (ca[0].indexOf(WILDCARD) >= 0
					|| ca[2].indexOf(WILDCARD) >= 0)
			{
				valid = true;
			}
			break;
		default:
			valid = true;
			break;
		}
		return valid;
	}

	/**
	 * Purge specified number of days from specified channel
	 * @param channel
	 * @param days
	 */
	public void purge(String channel, int days)
	{
		input.purgeTables(channel, days, this);
	}

	/**
	 * Purge specified number of days from specified channels
	 * @param chx the channel expression
	 * @param days the number of days
	 * @param delay the delay in milliseconds between channels
	 */
	public void purgeChannels(String chx, int days, long delay)
	{
		String ch = chx;
		try
		{
			List<String> channelList = channels.getChannelCodes(chx);
			if (channelList == null || channelList.size() == 0)
			{
				winston.getLogger().info("purgeChannels: no channels found (" + chx + ")");
			}
			else
			{
				for (int i = 0; i < channelList.size(); i++)
				{
					ch = channelList.get(i);
					if (i != 0 && delay != 0)
						Thread.sleep(delay);
					purge(ch, days);
					winston.getLogger().info("Purged channel: " + ch);
				}
			}
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Error during purgeChannels(" + ch + ")", e);
		}
	}

	/**
	 * Leaving as-is for old importer.
	 * @param day
	 * @param ch
	 * @return true if successful, false otherwise
	 */
	public boolean repairChannel(String day, String ch)
	{
		try
		{
			winston.useDatabase(ch);
			boolean fix = false;
			Statement st = winston.getStatement();
			
			winston.getLogger().info("Checking: " + ch + " " + day);
			ResultSet rs = st.executeQuery("CHECK TABLE `" + ch + "$$" + day + "` FAST QUICK");
			rs.next();
			String s = rs.getString("Msg_text");
			if (s.endsWith("doesn't exist"))
			{
				winston.getLogger().info(ch + " wave table doesn't exist.");
				return true;
			}
			if (!s.equals("Table is already up to date"))
				fix = true;
			
			rs = st.executeQuery("CHECK TABLE `" + ch + "$$H" + day + "` FAST QUICK");
			rs.next();
			s = rs.getString("Msg_text");
			if (s.endsWith("doesn't exist"))
			{
				winston.getLogger().info(ch + " helicorder table doesn't exist.");
				return true;
			}
			// TODO: check table existence
			if (!rs.getString("Msg_text").equals("Table is already up to date"))
				fix = true;
			
			if (fix)
			{
				winston.getLogger().info("Repairing: " + ch);
//				winston.getStatement().execute("REPAIR TABLE " + ch + "$$" + day + " QUICK");
//				winston.getStatement().execute("REPAIR TABLE " + ch + "$$H" + day + " QUICK");
				winston.getStatement().execute("REPAIR TABLE `" + ch + "$$" + day + "`");
				winston.getStatement().execute("REPAIR TABLE `" + ch + "$$H" + day + "`");
			}
			return true;
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Failed to repair: " + ch);
		}
		return false;
	}
	
	/**
	 * Repair specified channel for specified day
	 * If channel unspecified, repair all
	 * @param day
	 * @param chString channel to repair; null for all
	 */
	public void repair(String day, String chString)
	{
		if (chString == null)
		{
			List<Channel> chs = channels.getChannels();
			for (Channel ch : chs)
				repairChannel(day, ch.getCode());
		}
		else
		{
			repairChannel(day, chString);
		}
	}

	/**
	 * Check on specified table/database
	 * @param database name of database
	 * @param table name of table
	 * @return true if OK, false otherwise
	 * @throws SQLException
	 */
	public boolean checkTable(String database, String table) throws SQLException
	{
		ResultSet rs = winston.getStatement().executeQuery("CHECK TABLE `" + table + "` FAST QUICK");
		rs.next();
		String s = rs.getString("Msg_text");
		if (s.endsWith("doesn't exist"))
		{
			winston.getLogger().info(table + " doesn't exist.");
			return false;
		}
		return s.equals("Table is already up to date");
	}
	
	/**
	 * Attempts to repair a table.
	 * @param database the database
	 * @param table the table name
	 * @return flag indicating a whether table is healthy
	 */
	public boolean repairTable(String database, String table)
	{
		try
		{
			winston.useDatabase(database);
			winston.getLogger().info("Checking table: " + table);
			boolean ct = checkTable(database, table);
			if (ct == true)
				return true;
			
			winston.getLogger().info("Repairing table: " + table);
			winston.getStatement().execute("REPAIR TABLE `" + table + "` QUICK");
			ct = checkTable(database, table);
			if (ct == true)
				return true;

			winston.getLogger().info("Still broken, attempting further repair: " + table);
			winston.getStatement().execute("REPAIR TABLE `" + table + "` QUICK USE_FRM");
			return checkTable(database, table);
		}
		catch (Exception e)
		{
			winston.getLogger().log(Level.SEVERE, "Failed to repair: " + table);
		}
		return false;
	}
	
	/** Use with extreme caution.
	 */
	public void deleteWinston()
	{
		try
		{
			List<Channel> chs = channels.getChannels();
			for (Channel ch : chs)
				deleteChannel(ch.getCode());
			winston.getStatement().execute("DROP DATABASE `" + winston.databasePrefix + "_ROOT`");	
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static void printUsage(int status, String cmd)
	{
		if (status == 2)
			System.err.println("Missing or invalid command arguments for command (" + cmd + ")");
		System.out.println(
"Winston Admin\n\n" +
"A collection of commands for administering a Winston database.\n" +
"Information about connecting to the Winston database must be present\n" +
"in Winston.config in the current directory.\n\n" +
"Usage:\n" +
"  java gov.usgs.winston.db.Admin [options] command [command arguments]\n" +
"\nValid options:\n" +
"  --delay seconds                 the delay between each channel for commands\n" +
"                                  for multiple channels\n" +
"\nValid commands:\n" +
"  --list                          lists all channels\n" +
"  --list times                    lists all channels with time span\n" +
"  --delete channel                delete the specified channel\n" +
"  --deletex SSSS$CCC$NN[$LL]      delete the specified channels where:\n" +
"                                  SSSS is the station,\n" +
"                                  CCC is the channel which may contain\n" +
"                                  a wild card (%),\n" +
"                                  SSSS is the station,\n" +
"                                  NN is the network,\n" +
"                                  LL is the optional location which may contain\n" +
"                                  a wild card (%)\n" +
"  --span                          recalculate table spans\n" +
"  --purge channel days            purge the specified channel for the\n" +
"                                  specified number of days\n" +
"  --purgex channel days           purge the specified channel for the\n" +
"                                  specified number of days where the channel\n" +
"                                  may contain a wild card (%) anywhere\n" +
"  --repair YYYY_MM_DD [channel]   repair all tables on given day\n" +
"                                  optionally, just repair the specified channel\n" +
//"  --deletewinston                 completely deletes all Winston databases\n" +
"");
		if (status != 0)
		{
			System.exit(status);
		}
	}
	
	/** 
	 * Main method
	 * @param args command-line args
	 */
	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			printUsage(-1, null);
		}
		
		int argIndex = 0;
		String cmd = null;
		String arg;
		int status = 0;
		final ConfigFile cf = new ConfigFile("Winston.config");
		final String driver = cf.getString("winston.driver");
		final String url = cf.getString("winston.url");
		final String db = cf.getString("winston.prefix");
		final Admin admin = new Admin(driver, url, db);
		long delay = 0;
		
		while (argIndex < args.length && status == 0)
		{
			arg = args[argIndex++];
			// first check for options
			if (arg.equals("--delay")) 
			{
				try
				{
					arg = args[argIndex++];
					// convert seconds to milliseconds
					delay = Integer.parseInt(arg) * 1000L;
				}
				catch (Exception e)
				{
					status = 2;
				}
			}
			// then check the command and command arguments
			else if (cmd != null || (cmd = arg).length() == 0)
			{
				// already had a command or empty command
				status = 2;
			}
			else if (cmd.equals("--span"))
			{
				admin.calculateTableSpans();
			}
			else if (cmd.equals("--list"))
			{
				boolean times = false;
				if (argIndex < args.length)
				{
					arg = args[argIndex++];
					if (arg.equals("times"))
						times = true;
					else
						status = 2;
				}
				if (status == 0)
					admin.listChannels(times);
			}
			else if (cmd.equals("--delete") || cmd.equals("--deletex"))
			{
				if (argIndex < args.length)
				{
					arg = args[argIndex++];
					if (cmd.equals("--deletex"))
					{
						if (admin.isChannelExpressionValid(arg))
						{
							admin.deleteChannels(arg, delay);
						}
						else
						{
							status = 3;
							System.err.println(
									"channel expresion may not have wild card for network or station ("
									+ arg + ")");
						}
					}
					else
					{
						admin.deleteChannel(arg);
					}
				}
				else
					status = 2;
			}
			else if (cmd.equals("--purge") || cmd.equals("--purgex"))
			{
				String channel = null;
				int days = 0;
				try
				{
					arg = args[argIndex++];
					channel = arg;
					arg = args[argIndex++];
					days = Integer.parseInt(arg);
					if (days <= 0)
					{
						status = 2;
					}
				}
				catch (Exception e)
				{
					status = 2;
				}

				if (status == 0)
				{
					if (cmd.equals("--purgex"))
						admin.purgeChannels(channel, days, delay);
					else
						admin.purge(channel, days);
				}
			}
			else if (cmd.equals("--repair"))
			{
				if (argIndex < args.length)
				{
					final String day = args[argIndex++];
					final String ch;
					if (argIndex < args.length)
						ch = args[argIndex++];
					else
						ch = null;
					admin.repair(day, ch);
				}
				else
					status = 2;
			}
			//		else if (cmd.equals("--deletewinston"))
			//		{
			//			admin.deleteWinston();
			//		}
			else
			{
				System.err.println("Invalid argument(" + argIndex + "): '" + arg + "'");
				status = 1;
			}
		}
		
		if (status != 0)
		{
			printUsage(status, cmd);
		}
	}
}
