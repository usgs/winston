package gov.usgs.winston.db;

import gov.usgs.util.ConfigFile;

import java.sql.SQLException;

/**
 * 
 *
 * @author Dan Cervelli
 */
public class Upgrade
{
	private WinstonDatabase winston;
	private Upgrader[] upgraders = new Upgrader[] {
			new Upgrader1_1_0to1_1_1(),
			new Upgrader1_0_1to1_1_0(),
			new Upgrader1_0_0to1_0_1() };
	
	public Upgrade()
	{
		ConfigFile cf = new ConfigFile("Winston.config");
		String driver = cf.getString("winston.driver");
		String url = cf.getString("winston.url");
		String db = cf.getString("winston.prefix");
		winston = new WinstonDatabase(driver, url, db);
	}
	
	abstract private class Upgrader
	{
		public String sourceVersion;
		public String destinationVersion;
		
		protected String description;
		
		public void success()
		{
			System.out.println("Upgrade [" + getVersionChangeString() + "] successful.");
		}
		
		public String getVersionChangeString()
		{
			return sourceVersion + " -> " + destinationVersion;
		}
		
		abstract public boolean checkSourceVersion();
		abstract public void upgrade();
	}
	
	private class Upgrader1_1_0to1_1_1 extends Upgrader
	{
		public Upgrader1_1_0to1_1_1()
		{
			sourceVersion = "1.1.0";
			destinationVersion = "1.1.1";
			description = getVersionChangeString() + "\n" + 
					"Updates channelmetadata table.\n" + 
					"Estimated execution time: under 1 second." +
					"Estimated execution time: under 1 second.";
		}

		public boolean checkSourceVersion()
		{
			return "1.1.0".equals(winston.getSchemaVersion());
		}
		
		public void upgrade()
		{
			try
			{
				System.out.println("Altering channelmetadata table...");
				winston.getStatement().execute("ALTER TABLE channelmetadata " +
						"DROP PRIMARY KEY, " +
						"ADD PRIMARY KEY (sid, name), " +
						"DROP cmid");
				
				System.out.println("Inserting new version information...");
				winston.getStatement().execute("INSERT INTO version VALUES ('1.1.1', NOW())");
				success();
			}
			catch (SQLException e)
			{
				System.err.println("There was an exception during upgrade.");
				e.printStackTrace();
			}
		}
	}
	
	private class Upgrader1_0_1to1_1_0 extends Upgrader
	{
		public Upgrader1_0_1to1_1_0()
		{
			sourceVersion = "1.0.1";
			destinationVersion = "1.1.0";
			description = getVersionChangeString() + "\n" + 
					"Creates instrument and channel metadata tables and modifies the existing channels table.\n" + 
					"Existing longitude and latitude columns in channels table will no longer be used.\n" +
					"Estimated execution time: under 1 second.";
		}

		public boolean checkSourceVersion()
		{
			return "1.0.1".equals(winston.getSchemaVersion());
		}
		
		public void upgrade()
		{
			try
			{
				System.out.println("Creating instruments table...");
				winston.getStatement().execute("CREATE TABLE instruments (" +
			    		"iid INT PRIMARY KEY AUTO_INCREMENT," +
			    		"name VARCHAR(128) UNIQUE, " +
			    		"description VARCHAR(255), " +
			    		"lon DOUBLE DEFAULT -999, " +
			    		"lat DOUBLE DEFAULT -999, " +
			    		"height DOUBLE DEFAULT -999, " +
			    		"timezone VARCHAR(128))");
				
				System.out.println("Altering channels table...");
				winston.getStatement().execute("ALTER TABLE channels " +
						"ADD COLUMN iid INT AFTER sid, " +
						"ADD COLUMN alias VARCHAR(255) AFTER et, " +
						"ADD COLUMN unit VARCHAR(255) AFTER alias, " +
						"ADD COLUMN linearA DOUBLE DEFAULT 1E300 AFTER unit, " +
						"ADD COLUMN linearB DOUBLE DEFAULT 1E300 AFTER linearA");
				
				System.out.println("Creating groups tables...");
				winston.getStatement().execute("CREATE TABLE grouplinks (glid INT PRIMARY KEY AUTO_INCREMENT, " +
						"sid INT, nid INT) " + winston.tableEngine);
			    winston.getStatement().execute("CREATE TABLE groupnodes (nid INT PRIMARY KEY AUTO_INCREMENT, " +
			    		"parent INT DEFAULT 0, " +
			    		"name CHAR(255), " +
			    		"open BOOL DEFAULT 0) " + winston.tableEngine);
				
			    System.out.println("Creating channel metadata table...");
			    winston.getStatement().execute("CREATE TABLE channelmetadata (" +
			    		"cmid INT PRIMARY KEY AUTO_INCREMENT, " +
			    		"sid INT, " +
			    		"name VARCHAR(255), " +
			    		"value TEXT) " + winston.tableEngine);
			    
			    System.out.println("Creating instrument metadata table...");
			    winston.getStatement().execute("CREATE TABLE instrumentmetadata (" +
			    		"imid INT PRIMARY KEY AUTO_INCREMENT, " +
			    		"iid INT, " +
			    		"name VARCHAR(255), " +
			    		"value TEXT) " + winston.tableEngine);
			    
				System.out.println("Inserting new version information...");
				winston.getStatement().execute("INSERT INTO version VALUES ('1.1.0', NOW())");
				success();
			}
			catch (SQLException e)
			{
				System.err.println("There was an exception during upgrade.");
				e.printStackTrace();
			}
		}
	}
	
	private class Upgrader1_0_0to1_0_1 extends Upgrader
	{
		public Upgrader1_0_0to1_0_1()
		{
			sourceVersion = "1.0.0";
			destinationVersion = "1.0.1";
			description = getVersionChangeString() + "\n" + 
					"Creates a table in the Winston root database to track schema version.\n" + 
					"Adds longitude and latitude columns to channels table.\n" + 
					"Estimated execution time: under 1 second.";
		}

		public boolean checkSourceVersion()
		{
			return "1.0.0".equals(winston.getSchemaVersion());
		}
		
		public void upgrade()
		{
			try
			{
				System.out.println("Creating version table...");
				winston.getStatement().execute("CREATE TABLE version (schemaversion VARCHAR(10), installtime DATETIME) " + winston.tableEngine);
				System.out.println("Altering channels table...");
				winston.getStatement().execute("ALTER TABLE channels ADD COLUMN lon DOUBLE DEFAULT -999, ADD COLUMN lat DOUBLE DEFAULT -999");
				System.out.println("Inserting old version information...");
				winston.getStatement().execute("INSERT INTO version VALUES ('1.0.0', '2000-01-01')");
				System.out.println("Inserting new version information...");
				winston.getStatement().execute("INSERT INTO version VALUES ('1.0.1', NOW())");
				success();
			}
			catch (SQLException e)
			{
				System.err.println("There was an exception during upgrade.");
				e.printStackTrace();
			}
		}
	}
	
	public Upgrader getUpgrader()
	{
		
		if (winston.checkConnect())
		{
			String currentSchemaVersion = winston.getSchemaVersion();
			for (int i = 0; i < upgraders.length; i++)
			{
				if (upgraders[i].sourceVersion.equals(currentSchemaVersion))
					return upgraders[i];
			}
		}
		
		return null;
	}
	
	public void doUpgrade()
	{
		Upgrader ugr = getUpgrader();
		if (ugr == null)
		{
			System.out.println("No upgrade necessary.");
		}
		else
		{
			System.out.println("Performing upgrade:");
			System.out.println(ugr.description);
			System.out.println();
			System.out.println("Starting upgrade --------------------------------------------------------");
			ugr.upgrade();
			System.out.println("Finished ----------------------------------------------------------------");
		}
	}
	
	public String getUpgraderDescription()
	{
		Upgrader ugr = getUpgrader();
		
		if (ugr == null)
			return "Winston schema up-to-date, no upgrades available.";
		else
			return ugr.description;
	}
	
	public static void main(String[] args)
	{
		Upgrade ug = new Upgrade();
		if (args.length == 1 && args[0].equals("--upgrade"))
		{
			ug.doUpgrade();
		}
		else
		{
			String currentSchemaVersion = ug.getCurrentVersion();
			System.out.println("Current Winston schema version: " + currentSchemaVersion);
			System.out.println();
			String upDescription = ug.getUpgraderDescription();
			System.out.println("Available upgrade:");
			System.out.println(upDescription);
			System.out.println();
			System.out.println("Run with '--upgrade' option to perform an upgrade.");
		}
	}

	public String getCurrentVersion() {
		if (winston.checkConnect())
			return winston.getSchemaVersion();
		else
			return null;
	}

	public boolean upgradeAvailable() {
		return getUpgrader() != null;
	}
}
