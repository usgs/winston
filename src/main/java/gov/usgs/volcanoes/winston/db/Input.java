package gov.usgs.volcanoes.winston.db;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.Arrays;
import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.time.CurrentTime;
import gov.usgs.volcanoes.core.time.J2kSec;

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
public class Input {
  class ChannelInputOptimizer {
    String code;
    TreeMap<Double, double[]> data;
    double t1;
    double t2;

    public ChannelInputOptimizer(final String c) {
      code = c;
      data = new TreeMap<Double, double[]>();
      t1 = Double.NaN;
      t2 = Double.NaN;
    }

    public double[] getData(final double j2k) {
      return data.get(j2k);
    }

    public boolean isInitialized() {
      return !Double.isNaN(t1);
    }

    // TODO: optimize. garbage generator.
    public void putData(final double j2k, final double[] d) {
      data.put(j2k, d);
      if (data.size() > 60) {
        final int numToDelete = data.size() - 30;
        final Iterator<Double> it = data.keySet().iterator();
        for (int i = 0; i < numToDelete; i++) {
          it.next();
          it.remove();
        }
      }
    }
  }

  public enum InputResult {
    ERROR, SUCCESS, SUCCESS_CREATED_TABLE
  };

  private static final Logger LOGGER = LoggerFactory.getLogger(Input.class);
  private final HashMap<String, ChannelInputOptimizer> channelOptimizers;
  private final HashSet<String> checkTableCache;

  private ChannelInputOptimizer currentLock;
  private final Data data;

  private final DateFormat dateFormat;

  private final ArrayList<String> locks = new ArrayList<String>(9);

  private int maxDays = 0;

  private WinstonDatabase winston;

  private boolean writeLocks = false;

  /*
   * protected void purgeTables(String code)
   * {
   * if (maxDays <= 0)
   * return;
   *
   * List list = getDayTables(code);
   * if (list.size() > maxDays)
   * {
   * int numToDelete = list.size() - maxDays;
   * for (int i = 0; i < numToDelete; i++)
   * {
   * String table = (String)list.get(i);
   * try
   * {
   * if (!winston.useDatabase(code))
   * return;
   * winston.getStatement().execute("DROP TABLE " + table);
   * String ss[] = table.split("\\$\\$");
   * winston.getStatement().execute("DROP TABLE " + ss[0] + "$$H" + ss[1]);
   * }
   * catch (Exception e)
   * {
   * winston.getLogger().severe("Could not drop old table: " + code +
   * ".  Are permissions set properly?");
   * }
   * }
   * try
   * {
   * String nextLowestTable = (String)list.get(numToDelete);
   * ResultSet rs = winston.getStatement().executeQuery("SELECT MIN(st) FROM " + nextLowestTable);
   * rs.next();
   * double t1 = rs.getDouble(1);
   * setTimeSpan(code, t1, Double.NaN);
   * rs.close();
   * }
   * catch (Exception e)
   * {
   * winston.getLogger().severe("Could not update span after dropping table: " + code);
   * }
   * }
   * }
   */

  public Input(final WinstonDatabase w) {
    winston = w;
    data = new Data(w);
    channelOptimizers = new HashMap<String, ChannelInputOptimizer>();
    checkTableCache = new HashSet<String>();
    dateFormat = new SimpleDateFormat("yyyy_MM_dd");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public void calculateSpan(final String code) {
    try {
      final List<String> dayList = getDayTables(code);
      double mint = 1E300;
      double maxt = -1E300;
      String table = dayList.get(0);
      ResultSet rs = winston.getStatement().executeQuery("SELECT MIN(st) FROM `" + table + "`");
      rs.next();
      mint = Math.min(mint, rs.getDouble(1));
      table = dayList.get(dayList.size() - 1);
      rs = winston.getStatement().executeQuery("SELECT MAX(et) FROM `" + table + "`");
      rs.next();
      maxt = Math.max(maxt, rs.getDouble(1));
      setTimeSpan(code, mint, maxt);
      rs.close();
    } catch (final Exception e) {
      LOGGER.error("Could not calculate span: {}", code);
    }
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
  private boolean checkTable(final String code, final String date) {
    final String table = code + "$$" + date;
    if (checkTableCache.contains(table)) {
      return true;
    }

    try {
      final ResultSet rs =
          winston.getStatement().executeQuery("SELECT COUNT(*) FROM `" + table + "`");
      final boolean result = rs.next();
      if (result) {
        checkTableCache.add(table);
      }
      rs.close();
      return result;
    } catch (final Exception e) {
      checkTableCache.clear();
      return createDayTable(code, date);
    }
  }

  private boolean createDayTable(final String code, final String date) {
    try {
      winston.getStatement()
          .execute("CREATE TABLE `" + code + "$$" + date + "` (" + "st DOUBLE PRIMARY KEY, "
              + "et DOUBLE, " + "sr DOUBLE, " + "datatype CHAR(3), " + "tracebuf BLOB) "
              + winston.tableEngine);

      winston.getStatement()
          .execute("CREATE TABLE `" + code + "$$H" + date + "` (" + "j2ksec DOUBLE PRIMARY KEY, "
              + "smin INT, " + "smax INT, " + "rcnt INT, " + "rsam DOUBLE) " + winston.tableEngine);

      purgeTables(code, maxDays);
      return true;
    } catch (final Exception ex) {
      LOGGER.error("Could not create day table: {}${}.  Are permissions set properly?", code, date);
    }
    return false;
  }

  private boolean createTable(final String code, final String date) {
    try {
      winston.getStatement()
          .execute("CREATE TABLE `" + code + "$$" + date + "` (" + "st DOUBLE PRIMARY KEY, "
              + "et DOUBLE, " + "sr DOUBLE, " + "datatype CHAR(3), " + "tracebuf BLOB) "
              + winston.tableEngine);
      winston.getStatement()
          .execute("CREATE TABLE `" + code + "$$H" + date + "` (" + "j2ksec DOUBLE PRIMARY KEY, "
              + "smin INT, " + "smax INT, " + "rcnt INT, " + "rsam DOUBLE) " + winston.tableEngine);
      // purgeTables(code, maxDays);
      return true;
    } catch (final Exception ex) {
      LOGGER.error("Could not create day table: {}${}.  Are permissions set properly?", code, date);
    }
    return false;
  }

  public List<String> getDayTables(final String code) {
    try {
      final ArrayList<String> list = new ArrayList<String>(10);
      if (!winston.useDatabase(code)) {
        return null;
      }
      final ResultSet rs = winston.getStatement().executeQuery("SHOW TABLES");
      while (rs.next()) {
        list.add(rs.getString(1));
      }
      rs.close();
      Collections.sort(list);
      final ArrayList<String> dayList = new ArrayList<String>(list.size() / 2);
      for (int i = 0; i < list.size(); i++) {
        final String table = list.get(i);
        final String day = table.substring(table.indexOf("$$") + 2);
        if (day.length() == 10 && day.charAt(4) == '_' && day.charAt(7) == '_'
            && Character.isDigit(day.charAt(0)) && Character.isDigit(day.charAt(9))) {
          dayList.add(table);
        }
      }
      return dayList;
    } catch (final Exception e) {
      LOGGER.error("Could not generate list of tables: {}", code);
    }
    return null;
  }

  /**
   * This one is used by ImportEW.
   *
   * @param tb
   * @return result
   */
  public InputResult inputTraceBuf(final TraceBuf tb) {
    InputResult result = InputResult.ERROR;
    try {
      final String code = tb.toWinstonString();
      ChannelInputOptimizer opt = channelOptimizers.get(code);
      if (opt == null) {
        opt = new ChannelInputOptimizer(code);
        channelOptimizers.put(code, opt);
        final double[] span = data.getTimeSpan(code);
        opt.t1 = span[0];
        opt.t2 = span[1];
      }

      winston.useDatabase(code);
      final double ts = tb.getStartTimeJ2K();
      final String date = dateFormat.format(J2kSec.asDate(ts));
      boolean createdTable = false;
      if (!tableExists(code, date)) {
        if (createTable(code, date)) {
          createdTable = true;
        } else {
          throw new Exception("Could not create table.");
        }
      }

      String table = code + "$$" + date;

      if (writeLocks && !locks.contains(table)) {
        currentLock = opt;
        locks.add(table);
        locks.add(code + "$$H" + date);
        winston.getStatement()
            .execute("LOCK TABLES " + table + " WRITE, " + code + "$$H" + date + " WRITE");
      }

      final PreparedStatement insert =
          winston.getPreparedStatement("INSERT IGNORE INTO `" + table + "` VALUES (?,?,?,?,?)");
      insert.setDouble(1, ts);
      insert.setDouble(2, tb.getEndTimeJ2K());
      insert.setDouble(3, tb.samplingRate());
      insert.setString(4, tb.dataType());
      final byte[] stripped = Arrays.trimToCapacity(tb.bytes, tb.bytes.length - 1);
      final byte[] compressed = Zip.compress(stripped);
      insert.setBytes(5, compressed);
      insert.executeUpdate();

      opt.t1 = Math.min(opt.t1, tb.getStartTimeJ2K());
      opt.t2 = Math.max(opt.t2, tb.getEndTimeJ2K());

      if (!writeLocks) {
        setTimeSpan(code, opt.t1, opt.t2);
        // setTimeSpan(code, opt.t1, opt.t2);
      }

      /// ----- now insert/update helicorder/rsam data
      table = code + "$$H" + date;
      final double fst = Math.floor(tb.getStartTimeJ2K());
      final double cet = Math.ceil(tb.getEndTimeJ2K());
      final double[][] heliList = new double[((int) Math.round(cet - fst)) + 1][];
      int j = 0;
      for (int i = (int) Math.round(fst); i <= (int) Math.round(cet); i++) {
        double[] d = opt.getData(i);
        if (d == null) {
          final boolean filled = false;
          /*
           * // needed for static importer
           * if (fillHeli)
           * {
           * ResultSet rs = winston.executeQuery("SELECT j2ksec, smin, smax, rcnt, rsam FROM " +
           * table + " WHERE j2ksec=" + i);
           * if (rs.next())
           * {
           * filled = true;
           * d = new double[] { rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4),
           * rs.getDouble(5) };
           * }
           * }
           */

          if (!filled) {
            d = new double[] {i, Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 0};
            winston.getStatement().execute("INSERT IGNORE INTO `" + table + "` VALUES (" + i + ","
                + Integer.MAX_VALUE + "," + Integer.MIN_VALUE + ",0,0)");
          }
        }
        heliList[j++] = d;
      }
      double st = tb.getStartTimeJ2K();
      final double dt = 1 / tb.samplingRate();
      for (int i = 0; i < tb.numSamples(); i++) {
        j = (int) (Math.floor(st) - fst);
        final double[] d = heliList[j];
        final int sample = tb.samples()[i];
        d[1] = Math.min(d[1], sample);
        d[2] = Math.max(d[2], sample);
        d[4] = (d[4] * d[3] + Math.abs(sample)) / (d[3] + 1);
        d[3] = d[3] + 1;
        st += dt;
      }

      for (int i = 0; i < heliList.length; i++) {
        final int j2k = (int) Math.round(fst + i);
        winston.getStatement()
            .execute("UPDATE `" + table + "` SET smin=" + (int) heliList[i][1] + ", smax="
                + (int) heliList[i][2] + ", rcnt=" + (int) heliList[i][3] + ", rsam="
                + heliList[i][4] + " WHERE j2ksec=" + j2k);
        opt.putData(j2k, heliList[i]);
      }
      result = createdTable ? InputResult.SUCCESS_CREATED_TABLE : InputResult.SUCCESS;
    } catch (final Exception e) {
      LOGGER.error("Could not insert TraceBuf: {}. ({})", tb, e.getLocalizedMessage());
    }
    return result;
  }

  /**
   * Inputs a TraceBuf into the Winston database.
   *
   * It seems that this method is slow on older machines. It's not clear that
   * it is the fault of the compress function. I've tried prepared
   * statements in some places and they seem to be marginally slower. There
   * probably will be a real speed increase if the optimization code could
   * be written to avoid extra garbage generation and didn't use TreeMap.
   *
   * TODO: this is still used by the static importers. Need to eliminate.
   *
   * @param tb the TraceBuf
   */
  public boolean inputTraceBuf(final TraceBuf tb, final boolean fillHeli) {
    try {
      final String code = tb.toWinstonString();
      ChannelInputOptimizer opt = channelOptimizers.get(code);
      if (opt == null) {
        opt = new ChannelInputOptimizer(code);
        channelOptimizers.put(code, opt);
        final double[] span = data.getTimeSpan(code);
        opt.t1 = span[0];
        opt.t2 = span[1];
      }

      winston.useDatabase(code);
      final double ts = tb.getStartTimeJ2K();
      final String date = dateFormat.format(J2kSec.asDate(ts));
      checkTable(code, date);
      String table = code + "$$" + date;
      if (writeLocks && !locks.contains(table)) {
        currentLock = opt;
        locks.add(table);
        locks.add(code + "$$H" + date);
        winston.getStatement()
            .execute("LOCK TABLES `" + table + "` WRITE, `" + code + "$$H" + date + "` WRITE");
      }

      final PreparedStatement insert =
          winston.getPreparedStatement("INSERT INTO `" + table + "` VALUES (?,?,?,?,?)");
      insert.setDouble(1, ts);
      insert.setDouble(2, tb.getEndTimeJ2K());
      insert.setDouble(3, tb.samplingRate());
      insert.setString(4, tb.dataType());
      final byte[] stripped = Arrays.trimToCapacity(tb.bytes, tb.bytes.length - 1);
      final byte[] compressed = Zip.compress(stripped);
      insert.setBytes(5, compressed);
      try {
        insert.executeUpdate();
      } catch (final SQLException ex) {
        if (ex.getMessage().startsWith("Duplicate entry")) {
          insert.close();
          return false;
        }
      }

      opt.t1 = Math.min(opt.t1, tb.getStartTimeJ2K());
      opt.t2 = Math.max(opt.t2, tb.getEndTimeJ2K());
      if (!writeLocks) {
        setTimeSpan(code, opt.t1, opt.t2);
      }

      /// ----- now insert/update helicorder/rsam data
      table = code + "$$H" + date;
      final double fst = Math.floor(tb.getStartTimeJ2K());
      final double cet = Math.ceil(tb.getEndTimeJ2K());
      final double[][] heliList = new double[((int) Math.round(cet - fst)) + 1][];
      int j = 0;
      for (int i = (int) Math.round(fst); i <= (int) Math.round(cet); i++) {
        double[] d = opt.getData(i);
        if (d == null) {
          boolean filled = false;
          if (fillHeli) {
            final ResultSet rs = winston.executeQuery(
                "SELECT j2ksec, smin, smax, rcnt, rsam FROM `" + table + "` WHERE j2ksec=" + i);
            if (rs.next()) {
              filled = true;
              d = new double[] {rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4),
                  rs.getDouble(5)};
            }
          }

          if (!filled) {
            d = new double[] {i, Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 0};

            // insert = winston.getPreparedStatement("INSERT IGNORE INTO " + table + " VALUES (?," +
            // Integer.MAX_VALUE + "," + Integer.MIN_VALUE + ",0,0)");
            // insert.setInt(1, i);
            // insert.executeUpdate();
            winston.getStatement().execute("INSERT IGNORE INTO `" + table + "` VALUES (" + i + ","
                + Integer.MAX_VALUE + "," + Integer.MIN_VALUE + ",0,0)");
          }
        }
        heliList[j++] = d;
      }
      double st = tb.getStartTimeJ2K();
      final double dt = 1 / tb.samplingRate();
      for (int i = 0; i < tb.numSamples(); i++) {
        j = (int) (Math.floor(st) - fst);
        final double[] d = heliList[j];
        final int sample = tb.samples()[i];
        d[1] = Math.min(d[1], sample);
        d[2] = Math.max(d[2], sample);
        d[4] = (d[4] * d[3] + Math.abs(sample)) / (d[3] + 1);
        d[3] = d[3] + 1;
        st += dt;
      }

      for (int i = 0; i < heliList.length; i++) {
        final int j2k = (int) Math.round(fst + i);
        // insert = winston.getPreparedStatement("UPDATE " + table + " SET smin=?, smax=?, rcnt=?,
        // rsam=? WHERE j2ksec=?");
        // insert.setInt(1, (int)heliList[i][1]);
        // insert.setInt(2, (int)heliList[i][2]);
        // insert.setInt(3, (int)heliList[i][3]);
        // insert.setDouble(4, heliList[i][4]);
        // insert.setInt(5, j2k);
        // insert.executeUpdate();
        winston.getStatement()
            .execute("UPDATE `" + table + "` SET smin=" + (int) heliList[i][1] + ", smax="
                + (int) heliList[i][2] + ", rcnt=" + (int) heliList[i][3] + ", rsam="
                + heliList[i][4] + " WHERE j2ksec=" + j2k);

        opt.putData(j2k, heliList[i]);
      }
      return true;
    } catch (final Exception e) {
      LOGGER.error("Could not insert TraceBuf: {}. ({})", tb, e.getLocalizedMessage());
    }
    return false;
  }

  /**
   * Purge tables.
   *
   * @param channel the channel
   * @param days the number of days
   * @return the Admin or null if none.
   */
  public Admin purgeTables(final String channel, final int days) {
    return purgeTables(channel, days, (Admin) null);
  }

  /**
   * Purge tables.
   *
   * @param channel the channel
   * @param days the number of days
   * @param admin the Admin or null to create one if needed.
   * @return the Admin or null if none.
   */
  public Admin purgeTables(final String channel, final int days, Admin admin) {
    if (days <= 0) {
      return admin;
    }

    final SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd");
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    final Date now = new Date(CurrentTime.getInstance().now());
    final Date then = new Date(now.getTime() - (days * 86400000L));

    LOGGER.info("Purging '{}' tables before: {}", channel, df.format(then));

    final List<String> list = getDayTables(channel);
    if (list == null || list.size() == 0) {
      LOGGER.info("No day tables found");
      return admin;
    }

    boolean deleted = false;
    boolean setTime = false;
    for (final String table : list) {
      final String ss[] = table.split("\\$\\$");

      if (df.format(then).compareTo(ss[1]) > 0) {
        try {
          if (!winston.useDatabase(channel)) {
            return admin;
          }

          checkTableCache.remove(table);

          winston.getStatement().execute("DROP TABLE `" + table + "`");
          winston.getStatement().execute("DROP TABLE `" + ss[0] + "$$H" + ss[1] + "`");
          deleted = true;
          LOGGER.info("Deleted table: {}", table);
        } catch (final Exception e) {
          LOGGER.error("Could not drop old table: {}.  Are permissions set properly?", channel);
        }
      } else {
        if (deleted) {
          try {
            final String nextLowestTable = table;
            final ResultSet rs = winston.getStatement()
                .executeQuery("SELECT MIN(st) FROM `" + nextLowestTable + "`");
            rs.next();
            final double t1 = rs.getDouble(1);
            setTimeSpan(channel, t1, Double.NaN);
            rs.close();
            setTime = true;
          } catch (final Exception e) {
            LOGGER.error("Could not update span after dropping table: ", channel);
          }
        }
        break;
      }
    }
    if (deleted && !setTime) {
      // must have deleted all of the tables, just delete the channel entirely
      LOGGER.info("Permanently deleting channel: {}", channel);
      if (admin == null) {
        admin = new Admin(winston);
      }
      admin.deleteChannel(channel);
    }
    return admin;
  }

  /**
   * Sets the maximum number of days (tables) allowed after a TraceBuf is
   * put into the table. WARNING: if the value is >0 then Import WILL drop
   * tables, data will be lossed.
   *
   * @param i the maximum number of days
   */
  public void setMaxDays(final int i) {
    if (i >= 0) {
      maxDays = i;
    }
  }

  public void setTimeSpan(final String code, final double st, final double et) {
    if (!winston.checkConnect()) {
      return;
    }
    try {
      final ChannelInputOptimizer opt = channelOptimizers.get(code);

      if (!Double.isNaN(st)) {
        if (opt != null) {
          opt.t1 = st;
        }
        winston.getStatement().execute("UPDATE `" + winston.databasePrefix
            + "_ROOT`.channels SET st=" + st + " WHERE code='" + code + "'");
      }
      if (!Double.isNaN(et)) {
        if (opt != null) {
          opt.t2 = et;
        }
        winston.getStatement().execute("UPDATE `" + winston.databasePrefix
            + "_ROOT`.channels SET et=" + et + " WHERE code='" + code + "'");
      }
    } catch (final Exception e) {
      LOGGER.error("Could not set time span for channel: {}. ({})", code, e.getLocalizedMessage());
    }
  }

  public void setWinston(final WinstonDatabase db) {
    winston = db;
  }

  public void setWriteLock(final boolean b) {
    writeLocks = b;
  }

  private boolean tableExists(final String code, final String date) {
    final String table = code + "$$" + date;
    if (checkTableCache.contains(table)) {
      return true;
    }

    try {
      final ResultSet rs =
          winston.getStatement().executeQuery("SELECT COUNT(*) FROM `" + table + "`");
      final boolean result = rs.next();
      if (result) {
        checkTableCache.add(table);
      }
      rs.close();
      return result;
    } catch (final Exception e) {
    }
    // checkTableCache.clear();
    // return createDayTable(code, date);
    return false;
  }

  public void unlockTables() {
    if (currentLock == null) {
      return;
    }
    try {
      winston.getStatement().execute("UNLOCK TABLES");
      setTimeSpan(currentLock.code, currentLock.t1, currentLock.t2);
      currentLock = null;
      writeLocks = false;
      locks.clear();
    } catch (final SQLException e) {
      LOGGER.error("Exception while unlocking tables. ({})", e.getLocalizedMessage());
    }
  }
}
