package gov.usgs.volcanoes.winston.db;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.set.MapBackedSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.Deflater;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.core.time.CurrentTime;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;

/**
 * TraceBuf input functions for use by ImportEW.
 *
 *
 * @author Dan Cervelli
 * @author Joshua Doubleday
 */
public class InputEW {

  private static final Logger LOGGER = LoggerFactory.getLogger(InputEW.class);

  public static class InputResult {
    public enum Code {
      NO_CODE, ERROR_INPUT, ERROR_NULL_TRACEBUF, ERROR_DUPLICATE, ERROR_UNKNOWN, ERROR_DATABASE, ERROR_NO_WINSTON, ERROR_CHANNEL, ERROR_TIME_SPAN, ERROR_HELICORDER, SUCCESS, SUCCESS_CREATED_TABLE, SUCCESS_HELICORDER, SUCCESS_TIME_SPAN
    }

    public Code code;
    public TraceBuf traceBuf;
    public double failedHeliJ2K;

    public InputResult(final Code c, final TraceBuf tb) {
      code = c;
      traceBuf = tb;
    }
  }

  private static class HeliFields {
    public static final int J2KSEC = 0;
    public static final int SMIN = 1;
    public static final int SMAX = 2;
    public static final int RCNT = 3;
    public static final int RSAM = 4;
    public static final int MEAN = 5;
    public static final int MU = 6; // DC offset
    public static final int WEIGHTED_RSAM = 7; // DC offset removed
  }

  private WinstonDatabase winston;
  private final DateFormat dateFormat;

  private Logger logger;

  /*
   * These two static fields are for optimization purposes. They are static
   * and synchronized so multiple instances of this class can modify them
   * simultaneously.
   */
  private static Set<String> checkTableCache;
  private static Map<String, double[]> channelTimeSpans;

  // TODO: make this synchronized too?
  private final Map<String, SortedMap<Double, double[]>> channelHelicorderRows;
  private final Map<String, Integer> channelSid;

  private int maxRows = 300;
  private int numRowsToDelete = 60;
  private boolean enableValarmView = false;

  /**
   * Constructs a new Input2.
   *
   * @param w
   */
  @SuppressWarnings("unchecked")
  public InputEW(final WinstonDatabase w) {
    setWinston(w);
    checkTableCache =
        MapBackedSet.decorate(Collections.synchronizedMap(new LRUMap(w.cacheCap, true)));

    channelTimeSpans = Collections.synchronizedMap(new HashMap<String, double[]>());
    channelHelicorderRows = new HashMap<String, SortedMap<Double, double[]>>();
    channelSid = new HashMap<String, Integer>();
    dateFormat = new SimpleDateFormat("yyyy_MM_dd");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  /**
   * Set the winston database for this inputter.
   *
   * @param db
   *          the winston database;
   */
  public void setWinston(final WinstonDatabase db) {
    winston = db;
  }

  public void setRowParameters(final int mr, final int nd) {
    maxRows = mr;
    numRowsToDelete = nd;
  }

  private List<String> getDayTables(final String code) {
    final ArrayList<String> list = new ArrayList<String>(10);
    try {
      final ResultSet rs = winston.getStatement().executeQuery("SHOW TABLES");
      while (rs.next())
        list.add(rs.getString(1));
      rs.close();

      Collections.sort(list);

      final ArrayList<String> dayList = new ArrayList<String>(list.size() / 2);
      for (final String table : list) {
        final String day = table.substring(table.indexOf("$$") + 2);
        if (day.length() == 10 && day.charAt(4) == '_' && day.charAt(7) == '_'
            && Character.isDigit(day.charAt(0)) && Character.isDigit(day.charAt(9)))
          dayList.add(table);
      }
      return dayList;
    } catch (final Exception e) {
      LOGGER.error("Could not get list of tables for channel: {}. ({})", code, e);
    }
    return null;
  }

  public void purgeTables(final String channel, final int days) {
    if (days <= 0)
      return;

    if (!winston.checkConnect())
      return;

    if (!winston.useDatabase(channel))
      return;

    final List<String> list = getDayTables(channel);
    if (list == null)
      return;

    final Date now = new Date(CurrentTime.getInstance().now());
    final Date then = new Date(now.getTime() - (days * 86400000L));
    final String thenString = Time.format(WinstonDatabase.WINSTON_TABLE_DATE_FORMAT, then);

    winston.getLogger().info("Purging '" + channel + "' tables before: " + thenString);

    boolean deleted = false;
    boolean setTime = false;
    for (final String table : list) {
      final String ss[] = table.split("\\$\\$");

      if (thenString.compareTo(ss[1]) > 0) {
        try {
          checkTableCache.remove(table);
          winston.getStatement().execute("DROP TABLE `" + table + "`");
          winston.getStatement().execute("DROP TABLE `" + ss[0] + "$$H" + ss[1] + "`");

          deleted = true;
          winston.getLogger().info("Deleted table: " + table);
        } catch (final Exception e) {
          winston.getLogger()
              .severe("Could not drop old table: " + channel + ".  Are permissions set properly?");
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
            winston.getLogger().severe("Could not update span after dropping table: " + channel);
          }
        }
        break;
      }
    }
    if (deleted && !setTime) {
      // must have deleted all of the tables, just delete the channel
      // entirely
      winston.getLogger().info("Permanently deleting channel: " + channel);
      new Admin(winston).deleteChannel(channel);
    }
  }

  /**
   * Creates a day table.
   *
   * TODO: fix view out of order data bug
   *
   * @param code
   *          the code of the new table
   * @param date
   *          the date of the new table
   * @return success/failure indication
   */
  private boolean createDayTable(final String code, final String date) {
    try {
      final double prevDayJ2k = J2kSec.fromDate(dateFormat.parse(date)) - (24 * 3600);
      final String prevDate = dateFormat.format(J2kSec.asDate(prevDayJ2k));

      final String waveTable = code + "$$" + date;
      final String heliTable = code + "$$H" + date;
      final String waveTableLast = code + "$$" + prevDate;
      final String heliTableLast = code + "$$H" + prevDate;
      final String waveTableall = code + "$$" + "past2days";
      final String heliTableall = code + "$$H" + "past2days";

      winston.getStatement()
          .execute("CREATE TABLE `" + waveTable + "` (st DOUBLE PRIMARY KEY, et DOUBLE, sr DOUBLE, "
              + "datatype CHAR(3), tracebuf BLOB) " + winston.tableEngine);

      System.out
          .println("CREATE TABLE `" + waveTable + "` (st DOUBLE PRIMARY KEY, et DOUBLE, sr DOUBLE, "
              + "datatype CHAR(3), tracebuf BLOB) " + winston.tableEngine);

      winston.getStatement().execute(
          "CREATE TABLE `" + heliTable + "` (j2ksec DOUBLE PRIMARY KEY, smin INT, smax INT, "
              + "rcnt INT, rsam DOUBLE) " + winston.tableEngine);

      if (enableValarmView) {
        // if there is data from the previous day, we want to union it
        // into our
        // view, otherwise, setup views into current days data
        LOGGER.info("Creating VIEWs for VAlarm: {}", heliTableall);

        String sql =
            "CREATE or REPLACE VIEW `" + waveTableall + "` AS SELECT * FROM `" + waveTable + "`";
        if (tableExists(waveTableLast))
          sql += " UNION ALL select * from `" + waveTableLast + "`";
        winston.getStatement().execute(sql);

        sql = "CREATE or REPLACE VIEW `" + heliTableall + "` AS SELECT * FROM `" + heliTable + "`";
        if (tableExists(heliTableLast))
          sql += " UNION ALL select * from `" + heliTableLast + "`";
        winston.getStatement().execute(sql);
      }

      return true;
    } catch (final Exception ex) {
      LOGGER.error("Could not create day table: '{}${}'. ({})", code, date, ex);
    }
    return false;
  }

  /**
   * Checks if a table exists.
   *
   * @param code
   *          the code to check
   * @param date
   *          the date to check
   * @return indicator of table existence
   */
  private boolean tableExists(final String code, final String date) {
    return tableExists(code + "$$" + date);
  }

  /*
   * Checks if a table exists.
   *
   * @param code the code to check
   *
   * @param date the date to check
   *
   * @return indicator of table existence
   */
  private boolean tableExists(final String table) {

    if (checkTableCache.contains(table))
      return true;

    try {
      final ResultSet rs = winston.getStatement().executeQuery("SHOW TABLES LIKE '" + table + "'");
      final boolean result = rs.next();
      if (result) {
        checkTableCache.add(table);
        rs.close();
      }
      return result;
    } catch (final Exception e) {
    }
    return false;
  }

  /**
   * Updates the time span of a channel to include a given start and end time.
   *
   * TODO: preparedStatements
   *
   * @param channel
   *          the channel
   * @param st
   *          the start time
   * @param et
   *          the end time
   */
  private void setTimeSpan(final String channel, final double st, final double et)
      throws SQLException {
    final double[] d = channelTimeSpans.get(channel);
    if (!Double.isNaN(st)) {
      if (d != null)
        d[0] = st;
      winston.getStatement().execute("UPDATE `" + winston.databasePrefix + "_ROOT`.channels SET st="
          + st + " WHERE code='" + channel + "'");
    }
    if (!Double.isNaN(et)) {
      if (d != null)
        d[1] = et;
      winston.getStatement().execute("UPDATE `" + winston.databasePrefix + "_ROOT`.channels SET et="
          + et + " WHERE code='" + channel + "'");
    }
  }

  /**
   * Gets the current time span of the channel. Supercedes the version in
   * Data.java for optimization reasons.
   *
   * @param channel
   * @return
   */
  private double[] getTimeSpan(final String channel) {
    double[] d = channelTimeSpans.get(channel);
    if (d != null)
      return d;

    try {
      final ResultSet rs = winston.getStatement().executeQuery("SELECT st, et FROM `"
          + winston.databasePrefix + "_ROOT`.channels WHERE code='" + channel + "'");
      d = new double[] {Double.NaN, Double.NaN};
      if (rs.next()) {
        d[0] = rs.getDouble(1);
        d[1] = rs.getDouble(2);
      }
      rs.close();
      channelTimeSpans.put(channel, d);
      return d;
    } catch (final Exception e) {
      LOGGER.error("Could not get time span for channel: {}. ({})", channel, e);
    }
    return null;
  }

  /**
   * Gets a helicorder row. This function MUST be called before
   * updateHelicorderRow because it is responsible for creating the blank row
   * if no existing data can be found.
   *
   * @param table
   * @param j2ksec
   * @return
   */
  private double[] getHelicorderRow(final String channel, final double j2ksec,
      final boolean useDB) {
    SortedMap<Double, double[]> rows = channelHelicorderRows.get(channel);
    if (rows == null) {
      rows = new TreeMap<Double, double[]>();
      channelHelicorderRows.put(channel, rows);
    }
    double[] d = rows.get(j2ksec);
    if (d != null)
      return d;

    final String date = dateFormat.format(J2kSec.asDate(j2ksec));
    final String table = channel + "$$H" + date;

    if (useDB) {
      try {
        final ResultSet rs = winston.getStatement().executeQuery(
            "SELECT j2ksec, smin, smax, rcnt, rsam FROM `" + table + "` WHERE j2ksec=" + j2ksec);
        if (rs.next())
          d = new double[] {rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4),
              rs.getDouble(5), 0, 0, 0};
        rs.close();
      } catch (final Exception e) {
        LOGGER.warn("Could not get helicorder row: {}", e.getMessage());
      }
    }

    if (d == null)
      d = new double[] {j2ksec, Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 0, 0, 0, 0};

    rows.put(j2ksec, d);
    if (rows.size() > maxRows) {
      for (int i = 0; i < numRowsToDelete; i++)
        rows.remove(rows.firstKey());
    }
    return d;
  }

  private double getRSAMMu(final String channel, final double j2ksec, int delta,
      final int duration) {
    final SortedMap<Double, double[]> rows = channelHelicorderRows.get(channel);
    if (rows == null)
      return Double.NaN;

    delta = -delta;
    double sampleSum = 0;
    double sampleCount = 0;
    for (int k = delta - duration; k < delta; k++) {
      final double[] hr = rows.get(j2ksec + k);
      if (hr != null) {
        sampleSum += hr[HeliFields.MEAN] * hr[HeliFields.RCNT];
        sampleCount += hr[HeliFields.RCNT];
      }
    }

    if (sampleCount != 0)
      return sampleSum / sampleCount;
    else
      return 0;
  }

  private PreparedStatement getInputStatement(final String table, final TraceBuf tb) {
    if (tb == null) {
      LOGGER.error("null tb");
      return null;
    }

    final PreparedStatement insert =
        winston.getPreparedStatement("INSERT INTO `" + table + "` VALUES (?,?,?,?,?);");
    if (insert == null) {
      LOGGER.error("Call to getPreparedStatement returned null.");
      return null;
    }

    try {
      insert.setDouble(1, tb.getStartTimeJ2K());
      insert.setDouble(2, tb.getEndTimeJ2K());
      insert.setDouble(3, tb.samplingRate());
      insert.setString(4, tb.dataType());
      final byte[] compressed =
          Util.compress(tb.bytes, Deflater.BEST_SPEED, 0, tb.bytes.length - 1);
      insert.setBytes(5, compressed);
    } catch (final SQLException e) {
      LOGGER.error("Could not create prepared statement: {}.({})", tb, e);
    }

    return insert;
  }

  /**
   * Updates a helicorder row.
   *
   * @param channel
   * @param date
   * @param tb
   * @throws SQLException
   */
  private void updateHelicorderData(final Set<Double> modifiedRows, final String channel,
      final String date, final TraceBuf tb, final boolean computeRsam, final int delta,
      final int duration, final boolean useDB) // throws SQLException
  {
    // TODO: optimize/simplify
    final double fst = Math.floor(tb.getStartTimeJ2K());
    final double cet = Math.ceil(tb.getEndTimeJ2K());
    final double[][] heliList = new double[((int) Math.round(cet - fst)) + 1][];
    int j = 0;
    for (int i = (int) Math.round(fst); i <= (int) Math.round(cet); i++) {
      modifiedRows.add((double) i);
      heliList[j] = getHelicorderRow(channel, i, useDB);
      if (computeRsam) {
        final double mu = getRSAMMu(channel, i, delta, duration);
        heliList[j][HeliFields.MU] = mu;
      }
      j++;
    }
    double st = tb.getStartTimeJ2K();
    final double dt = 1 / tb.samplingRate();

    for (int i = 0; i < tb.numSamples(); i++) {
      j = (int) (Math.floor(st) - fst);
      final double[] d = heliList[j];
      final int sample = tb.samples()[i];
      d[HeliFields.SMIN] = Math.min(d[HeliFields.SMIN], sample);
      d[HeliFields.SMAX] = Math.max(d[HeliFields.SMAX], sample);
      if (computeRsam) {
        // original RSAM
        d[HeliFields.RSAM] =
            (d[HeliFields.RSAM] * d[HeliFields.RCNT] + Math.abs(sample)) / (d[HeliFields.RCNT] + 1);
        // average of samples
        d[HeliFields.MEAN] =
            (d[HeliFields.MEAN] * d[HeliFields.RCNT] + sample) / (d[HeliFields.RCNT] + 1);
        d[HeliFields.WEIGHTED_RSAM] =
            (d[HeliFields.WEIGHTED_RSAM] * d[HeliFields.RCNT] + Math.abs(sample - d[HeliFields.MU]))
                / (d[HeliFields.RCNT] + 1);
        d[HeliFields.RCNT]++;
      }
      st += dt;
    }
  }

  /**
   * Writes helicorder data to the database. In case of failure, returns the
   * j2k that failed. Returns NaN on success.
   *
   * @param channel
   * @param modifiedRows
   * @return
   */
  private double writeHelicorderData(final String channel, final Set<Double> modifiedRows) // throws
  // SQLException
  {
    for (final double j2k : modifiedRows) {
      final String date = dateFormat.format(J2kSec.asDate(j2k));
      final String table = channel + "$$H" + date;

      final double[] row = getHelicorderRow(channel, j2k, false);
      final String sql = String.format(
          "INSERT INTO `%s` (j2ksec, smin, smax, rcnt, rsam) "
              + "VALUES (%f,%d,%d,%d,%f) ON DUPLICATE KEY UPDATE "
              + "smin=VALUES(smin), smax=VALUES(smax), rcnt=VALUES(rcnt), rsam=VALUES(rsam)",
          table, j2k, (int) row[HeliFields.SMIN], (int) row[HeliFields.SMAX],
          (int) row[HeliFields.RCNT], row[HeliFields.WEIGHTED_RSAM]);
      try {
        winston.getStatement().execute(sql);
      } catch (final SQLException ex) {
        LOGGER.warn("Could not write helicorder row: {}", ex.getMessage());
        LOGGER.warn("SQL: {}", sql);
        return j2k;
      }
    }

    return Double.NaN;
  }

  public boolean rederive(final List<TraceBuf> tbs, final boolean computeRsam, final int delta,
      final int duration) {
    if (tbs == null || tbs.size() == 0)
      return false;

    if (!winston.checkConnect())
      return false;

    final String channel = tbs.get(0).toWinstonString();

    if (!winston.useDatabase(channel))
      return false;

    final SortedSet<Double> modifiedHeliRows = new TreeSet<Double>();
    final Iterator<TraceBuf> it = tbs.iterator();
    while (it.hasNext()) {
      final TraceBuf tb = it.next();
      if (tb == null)
        continue;

      if (!tb.toWinstonString().equals(channel))
        continue;

      final double ts = tb.getStartTimeJ2K();
      final String date = dateFormat.format(J2kSec.asDate(ts));

      updateHelicorderData(modifiedHeliRows, channel, date, tb, computeRsam, delta, duration,
          false);
    }

    return Double.isNaN(writeHelicorderData(channel, modifiedHeliRows));
  }

  private List<InputResult> getError(final InputResult.Code code) {
    final ArrayList<InputResult> list = new ArrayList<InputResult>(1);
    list.add(new InputEW.InputResult(code, null));
    return list;
  }

  /**
   * Inserts multiple TraceBufs for a single channel into the database. This
   * uses optimizations that require that all of the TraceBufs be from the
   * same channel. Using this function with different channels will produce
   * unstable results.
   *
   * This function returns either null if the input is null or of size 0, if
   * the Winston connection can not be established, if the station time span
   * can be calculated (bad database), or the channel's database can not be
   * used (again, bad database). Otherwise it returns a list of InputResults
   * for each TraceBuf and one that specifies the result of the time span
   * update.
   *
   * @param tbs
   *          the list of TraceBufs to insert
   * @return the result or null (see above)
   */
  public List<InputResult> inputTraceBufs(final List<TraceBuf> tbs, final boolean computeRsam,
      final int delta, final int duration) {
    if (tbs == null || tbs.size() == 0)
      return getError(InputResult.Code.ERROR_INPUT);

    if (!winston.checkConnect())
      return getError(InputResult.Code.ERROR_NO_WINSTON);

    final String channel = tbs.get(0).toWinstonString();
    final double[] span = getTimeSpan(channel);
    if (span == null)
      return getError(InputResult.Code.ERROR_TIME_SPAN);

    final double stBefore = span[0];

    if (!winston.useDatabase(channel))
      return getError(InputResult.Code.ERROR_DATABASE);

    final ArrayList<InputResult> results = new ArrayList<InputResult>(tbs.size() + 1);

    final SortedSet<Double> modifiedHeliRows = new TreeSet<Double>();

    final Iterator<TraceBuf> it = tbs.iterator();
    while (it.hasNext()) {
      boolean tableCreated = false;
      final TraceBuf tb = it.next();
      final InputResult result = new InputResult(InputResult.Code.NO_CODE, tb);
      if (tb == null) {
        result.code = InputResult.Code.ERROR_NULL_TRACEBUF;
      } else if (!tb.toWinstonString().equals(channel)) {
        result.code = InputResult.Code.ERROR_CHANNEL;
      }

      if (result.code != InputResult.Code.NO_CODE)
        continue;

      final double ts = tb.getStartTimeJ2K();
      final String date = dateFormat.format(J2kSec.asDate(ts));
      final String endDate = dateFormat.format(J2kSec.asDate(tb.getEndTimeJ2K() + 1));
      final String table = channel + "$$" + date;

      try {
        if (!tableExists(channel, date)) {
          createDayTable(channel, date);
          tableCreated = true;
        }
        if (!tableExists(channel, endDate)) {
          createDayTable(channel, endDate);
          tableCreated = true;
        }

        final PreparedStatement insert = getInputStatement(table, tb);

        try {
          insert.executeUpdate();
        } catch (final SQLException ex) {
          if (ex.getMessage().startsWith("Duplicate entry"))
            result.code = InputResult.Code.ERROR_DUPLICATE;
          else
            throw ex;
        }

        span[0] = Math.min(span[0], tb.getStartTimeJ2K());
        span[1] = Math.max(span[1], tb.getEndTimeJ2K());

        if (tb.samplingRate() > 2 && result.code != InputResult.Code.ERROR_DUPLICATE)
          updateHelicorderData(modifiedHeliRows, channel, date, tb, computeRsam, delta, duration,
              true);
      } catch (final SQLException ex) {
        result.code = InputResult.Code.ERROR_DATABASE;
        LOGGER.error("Could not insert trace buf: {}", ex);
      }

      if (result.code == InputResult.Code.NO_CODE) {
        if (tableCreated)
          result.code = InputResult.Code.SUCCESS_CREATED_TABLE;
        else
          result.code = InputResult.Code.SUCCESS;
      }

      results.add(result);
      tableCreated = false;
    }

    final InputResult heliResult = new InputResult(InputResult.Code.SUCCESS_HELICORDER, null);
    final double failed = writeHelicorderData(channel, modifiedHeliRows);
    if (!Double.isNaN(failed)) {
      heliResult.code = InputResult.Code.ERROR_HELICORDER;
      heliResult.failedHeliJ2K = failed;
    }

    results.add(heliResult);

    final InputResult spanResult = new InputResult(InputResult.Code.ERROR_TIME_SPAN, null);
    try {
      if (span[0] == stBefore)
        setTimeSpan(channel, Double.NaN, span[1]);
      else
        setTimeSpan(channel, span[0], span[1]);
      spanResult.code = InputResult.Code.SUCCESS_TIME_SPAN;
    } catch (final SQLException ex) {
      LOGGER.error("Could not set time span for channel: {}. ({})", channel, ex);
    }
    results.add(spanResult);

    return results;
  }

  private int getSid(final String c) throws Exception {
    winston.useRootDatabase();
    if (!channelSid.containsKey(c)) {
      final ResultSet rs = winston.getStatement().executeQuery(
          "SELECT sid FROM `" + winston.databasePrefix + "_ROOT`.channels WHERE code='" + c + "'");
      if (rs.next()) {
        final int sid = rs.getInt(1);
        channelSid.put(c, sid);
      }
    }

    return channelSid.get(c);
  }

  /**
   * Inserts multiple metadata updates
   *
   * This function returns either null if the input is null or of size 0, if
   * the Winston connection can not be established, or the channelmetadata
   * database can not be used (again, bad database). Otherwise it returns a
   * list of InputResults for each entry.
   *
   * @param channel
   * @param entries
   * @return the result or null (see above)
   */
  public void inputMetadata(final String channel, final Map<String, String> m) {

    if (!winston.checkConnect() || !winston.useRootDatabase())
      LOGGER.error("Can't update metadata: Can't connect to Winston");
    else {
      try {
        final int sid = getSid(channel);

        final PreparedStatement ps = winston.getPreparedStatement(
            "REPLACE INTO channelmetadata (sid, name, value) VALUES (?,?,?);");
        final Iterator<String> it = m.keySet().iterator();
        while (it.hasNext()) {
          final String name = it.next();
          ps.setInt(1, sid);
          ps.setString(2, name);
          ps.setString(3, m.get(name));

          ps.executeUpdate();
          LOGGER.error("Metadata updated for {}: {}={}", channel, name, m.get(name));
        }

        m.clear();
      } catch (final Exception e) {
        LOGGER.error("Can't update metadata: {}", e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public void setEnableValarmView(final boolean enableValarmView) {
    this.enableValarmView = enableValarmView;

  }

}
