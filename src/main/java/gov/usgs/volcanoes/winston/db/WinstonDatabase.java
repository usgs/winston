package gov.usgs.volcanoes.winston.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.Retriable;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;

/**
 * A class that manages a connection to a Winston database.
 *
 * TODO: set default st and et in channels table to 1E300, -1E300.
 *
 * @author Dan Cervelli
 */
public class WinstonDatabase {
  private static final Logger LOGGER = LoggerFactory.getLogger(WinstonDatabase.class);

  public static final String WINSTON_TABLE_DATE_FORMAT = "yyyy_MM_dd";
  public static final String CURRENT_SCHEMA_VERSION = "1.1.1";
  public static final long MAX_DAYS_UNLIMITED = Long.MAX_VALUE / Time.DAY_IN_MS;

  private static final String DEFAULT_DATABASE_PREFIX = "W";
  private static final String DEFAULT_CONFIG_FILENAME = "Winston.config";
  private static final int DEFAULT_CACHE_CAPACITY = 100;

  /** Connection to the Winston */
  private Connection winstonConnection;

  /** Statement for interacting with the Winston. */
  private Statement winstonStatement;

  /** The connection state with the Winston. */
  private boolean winstonConnected;

  public final String dbDriver;
  public final String dbURL;
  public final int cacheCap;
  public final String databasePrefix;
  public final String tableEngine;
  public final long maxDays;

  private final PreparedStatementCache preparedStatements;

  public WinstonDatabase(final String dbDriver, final String dbURL, final String databasePrefix) {
    this(dbDriver, dbURL, databasePrefix, DEFAULT_CACHE_CAPACITY);
  }

  public WinstonDatabase(final String dbDriver, final String dbURL, final String databasePrefix,
      final int cacheCap) {
    this(dbDriver, dbURL, databasePrefix, null, cacheCap);
  }

  public WinstonDatabase(final String dbDriver, final String dbURL, final String databasePrefix,
      final String tableEngine, final int cacheCap) {
    this(dbDriver, dbURL, databasePrefix, null, cacheCap, MAX_DAYS_UNLIMITED);
  }

  public WinstonDatabase(final String dbDriver, final String dbURL, final String databasePrefix,
      final String tableEngine, final int cacheCap, final long maxDays) {

    // Set default Locale to US. This ensures that decimals play well with the SQL standard. ie. no
    // decimal comma
    Locale.setDefault(Locale.US);

    this.dbDriver = dbDriver;
    this.dbURL = dbURL;
    this.cacheCap = cacheCap;
    this.databasePrefix = StringUtils.stringToString(databasePrefix, DEFAULT_DATABASE_PREFIX);
    this.tableEngine = (tableEngine == null) ? "" : (" ENGINE = " + tableEngine);
    this.maxDays = maxDays;

    preparedStatements = new PreparedStatementCache(this.cacheCap, true);
    connect();
  }

  private void connect() {
    winstonConnected = false;
    try {
      Class.forName(dbDriver).newInstance();
      DriverManager.setLoginTimeout(3);
      winstonConnection = DriverManager.getConnection(dbURL);
      winstonStatement = winstonConnection.createStatement();
      winstonConnected = true;
      preparedStatements.clear();
      LOGGER.info("Connected to database.");
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException("Could not load the database driver, check your CLASSPATH. ({})",
          e);
    } catch (final Exception e) {
      winstonConnection = null;
      winstonStatement = null;
      LOGGER.error("Could not connect to Winston.", e);
      winstonConnected = false;
    }
  }

  public void close() {
    if (!checkConnect())
      return;

    try {
      winstonStatement.close();
      winstonConnection.close();
      winstonConnected = false;
    } catch (final Exception e) {
      LOGGER.warn("Error closing database.  This is unusual, but not critical.");
    }
  }

  public boolean checkConnect() {
    if (winstonConnected)
      return true;
    else {
      try {
        new Retriable<Object>() {
          @Override
          public boolean attempt() throws UtilException {
            connect();
            return winstonConnected;
          }
        }.go();
      } catch (final UtilException e) {
        // Do nothing
      }
      return winstonConnected;
    }
  }

  public boolean connected() {
    return winstonConnected;
  }

  public Connection getConnection() {
    return winstonConnection;
  }

  public Statement getStatement() {
    return winstonStatement;
  }

  public Statement getNewStatement() throws SQLException {
    return winstonConnection.createStatement();
  }

  public String getSchemaVersion() {
    useRootDatabase();
    String sv = null;
    try {
      final ResultSet rs = winstonStatement
          .executeQuery("SELECT schemaversion FROM version ORDER BY installtime DESC LIMIT 1");
      rs.next();
      sv = rs.getString(1);
    } catch (final SQLException e) {
      sv = "1.0.0";
    }
    return sv;
  }

  public ResultSet executeQuery(final String sql) throws DbException {
    ResultSet rs = null;
    try {
      rs = new Retriable<ResultSet>() {
        @Override
        public void attemptFix() {
          close();
          connect();
        }

        @Override
        public boolean attempt() throws UtilException {
          try {
            result = winstonStatement.executeQuery(sql);
            return true;
          } catch (final SQLException e) {
            LOGGER.error("executeQuery() failed, SQL: {}. ({})", sql, e);
          }
          return false;
        }
      }.go();
    } catch (UtilException e) {
      throw new DbException(e.getLocalizedMessage());
    }

    if (rs == null) {
      throw new DbException("Cannot execute query");
    }

    return rs;
  }

  private void createTables() {
    try {
      getStatement().execute("CREATE TABLE instruments (iid INT PRIMARY KEY AUTO_INCREMENT,"
          + "name VARCHAR(250) UNIQUE, description VARCHAR(255), "
          + "lon DOUBLE DEFAULT -999, lat DOUBLE DEFAULT -999, "
          + "height DOUBLE DEFAULT -999, timezone VARCHAR(128)) " + tableEngine);

      getStatement().execute("CREATE TABLE channels (sid INT PRIMARY KEY AUTO_INCREMENT,"
          + "iid INT, code VARCHAR(50), st DOUBLE, et DOUBLE,"
          + " alias VARCHAR(255), unit VARCHAR(255), linearA DOUBLE DEFAULT 1E300, "
          + " linearB DOUBLE DEFAULT 1E300, "
          + " UNIQUE KEY (code) "
          + " ) " + tableEngine);

      getStatement().execute("CREATE TABLE version (" + "schemaversion VARCHAR(10), "
          + "installtime DATETIME) " + tableEngine);

      getStatement()
          .execute("INSERT INTO version VALUES ('" + CURRENT_SCHEMA_VERSION + "', NOW())");

      getStatement().execute("CREATE TABLE grouplinks (glid INT PRIMARY KEY AUTO_INCREMENT, "
          + "sid INT, nid INT) " + tableEngine);

      getStatement().execute("CREATE TABLE groupnodes (nid INT PRIMARY KEY AUTO_INCREMENT, "
          + "parent INT DEFAULT 0, " + "name CHAR(255), " + "open BOOL DEFAULT 0) " + tableEngine);

      getStatement().execute("CREATE TABLE channelmetadata (sid INT, name VARCHAR(249),"
          + " value TEXT, PRIMARY KEY (sid, name)) " + tableEngine);

      getStatement()
          .execute("CREATE TABLE instrumentmetadata (imid INT PRIMARY KEY AUTO_INCREMENT, "
              + "iid INT, " + "name VARCHAR(255), " + "value TEXT) " + tableEngine);

      getStatement().execute("CREATE TABLE supp_data (sdid INT NOT NULL AUTO_INCREMENT, "
          + "st DOUBLE NOT NULL, et DOUBLE, sdtypeid INT NOT NULL, "
          + "sd_short VARCHAR(90) NOT NULL, sd TEXT NOT NULL, PRIMARY KEY (sdid)) " + tableEngine);

      getStatement().execute("CREATE TABLE supp_data_type (sdtypeid INT NOT NULL AUTO_INCREMENT, "
          + "supp_data_type VARCHAR(20), supp_color VARCHAR(6) NOT NULL, draw_line TINYINT, PRIMARY KEY (sdtypeid), UNIQUE KEY (supp_data_type) ) "
          + tableEngine);

      getStatement().execute("CREATE TABLE supp_data_xref ( sdid INT NOT NULL, cid INT NOT NULL, "
          + "UNIQUE KEY (sdid,cid) ) " + tableEngine);
    } catch (final Exception e) {
      LOGGER.error("Could not create tables in WWS database.  Are permissions set properly? ({})",
          e);
    }
  }

  public boolean useRootDatabase() {
    return useDatabase("ROOT");
  }

  public boolean useDatabase(final String db) {
    if (!checkConnect())
      return false;

    try {
      try {
        winstonStatement.execute("USE `" + databasePrefix + "_" + db + "`");
      } catch (final SQLException e) {
        close();
        connect();
      }
      winstonStatement.execute("USE `" + databasePrefix + "_" + db + "`");
      return true;
    } catch (final SQLException e) {
      if (e.getMessage().indexOf("Unknown database") != -1)
        LOGGER.info("Attempt to use nonexistent database: {}", db);
      else
        LOGGER.error("Could not use database: {}. ({})", db, e);
    }
    return false;
  }

  public boolean checkDatabase() {
    if (!checkConnect())
      return false;

    try {
      boolean failed = false;
      try {
        getStatement().execute("USE " + databasePrefix + "_ROOT");
      } catch (final Exception e) {
        failed = true;
      }
      if (failed) {
        getStatement().execute("CREATE DATABASE `" + databasePrefix + "_ROOT`");
        getStatement().execute("USE `" + databasePrefix + "_ROOT`");
        LOGGER.info("Created new Winston database: {}", databasePrefix);
        createTables();
      }
      return true;
    } catch (final Exception e) {
      LOGGER.error("Could not locate or create WWS database.  Are permissions set properly?");
    }
    return false;
  }

  public boolean tableExists(final String db, final String table) {
    try {
      final ResultSet rs = getStatement().executeQuery(
          String.format("SELECT COUNT(*) FROM `%s_%s`.%s", databasePrefix, db, table));
      final boolean result = rs.next();
      rs.close();
      return result;
    } catch (final SQLException e) {
      LOGGER.info("SQLException: {}", e.getMessage());
    }
    return false;
  }

  public PreparedStatement getPreparedStatement(final String sql) {
    try {
      PreparedStatement ps = (PreparedStatement) preparedStatements.get(sql);
      if (ps == null) {
        ps = winstonConnection.prepareStatement(sql);
        preparedStatements.put(sql, ps);
        LOGGER.debug("Adding statement to cache({}/{}): {}", preparedStatements.size(),
            preparedStatements.maxSize(), sql);
      }
      return ps;
    } catch (final Exception e) {
      LOGGER.error("Could not prepare statement. ({})", e.getLocalizedMessage());
    }
    return null;
  }

  public static WinstonDatabase processWinstonConfigFile() {
    return processWinstonConfigFile(new ConfigFile(DEFAULT_CONFIG_FILENAME));
  }

  public static WinstonDatabase processWinstonConfigFile(final ConfigFile cf) {
    final String dbDriver = cf.getString("winston.driver");
    final String dbURL = cf.getString("winston.url");
    final String databasePrefix = cf.getString("winston.prefix");
    final String tableEngine = cf.getString("winston.tableEngine");
    final int cacheCap =
        StringUtils.stringToInt(cf.getString("winston.statementCacheCap"), DEFAULT_CACHE_CAPACITY);

    return new WinstonDatabase(dbDriver, dbURL, databasePrefix, tableEngine, cacheCap);
  }
}
