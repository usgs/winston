package gov.usgs.volcanoes.winston.db;

import java.io.IOException;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.RSAMData;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;
import gov.usgs.volcanoes.core.math.DownsamplingType;
import gov.usgs.volcanoes.core.time.CurrentTime;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;

/**
 * A class to handle data acquistion from a Winston database.
 *
 * @author Dan Cervelli
 */
public class Data {
  private static final Logger LOGGER = LoggerFactory.getLogger(Data.class);
  private static final double EPSILON = 0.01;

  private static final int ONE_DAY = 60 * 60 * 24;
  private final WinstonDatabase winston;
  private final Channels channels;
  private final DateFormat dateFormat;
  private String vdxName;

  /**
   * Constructor
   *
   * @param w
   *          WinstonDatabase
   */
  public Data(final WinstonDatabase winston) {
    this.winston = winston;
    vdxName = "";
    channels = new Channels(winston);
    dateFormat = new SimpleDateFormat("yyyy_MM_dd");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  /**
   * Get timespan for specific channel
   *
   * @param code
   *          channel code
   * @return Array of start and end times
   */
  public double[] getTimeSpan(final Scnl scnl) {
    return getTimeSpan(DbUtils.scnlAsWinstonCode(scnl));
  }


  /**
   * Get timespan for specific channel
   *
   * @param sid
   *          channel id
   * @return Array of start and end times
   */
  public double[] getTimeSpan(final int sid) {
    final String code = channels.getChannelCode(sid);
    return getTimeSpan(code);
  }


  /**
   * Get timespan for specific channel
   *
   * @param code
   *          channel code
   * @return Array of start and end times
   */
  public double[] getTimeSpan(final String code) {
    if (!winston.checkConnect())
      return new double[] {0,0};
    try {
      ResultSet rs = winston.getStatement().executeQuery("SELECT st, et FROM `"
          + winston.databasePrefix + "_ROOT`.channels WHERE code='" + code + "'");
      rs.next();
      double st = rs.getDouble(1);
      double et = rs.getDouble(2);
      st = applyLookback(st);
      double[] d = null;

      if (et > st) {
        d = new double[] {st, et};
      }

      rs.close();

      return d;
    } catch (final Exception e) {
      LOGGER.error("Could not get time span for channel: {}. ({})", code, e.getLocalizedMessage());
    }
    return new double[] {0,0};
  }

  /**
   * Get days between times t1 & t2
   *
   * @param t1
   *          start of timespan
   * @param t2
   *          end of timespan
   * @return List of strings representing each day between t1 & t2
   */
  private List<String> daysBetween(final double t1, final double t2) {
    final ArrayList<String> result = new ArrayList<String>();
    double ct = t1;
    while (ct < t2 + ONE_DAY) {
      result.add(J2kSec.format(WinstonDatabase.WINSTON_TABLE_DATE_FORMAT, ct));
      ct += ONE_DAY;
    }
    return result;
  }

  /**
   * Get a list of days in a time span
   *
   * @param timeSpan timeSpan
   * @return List of strings representing each day in time span
   */
  private List<String> daysBetween(TimeSpan timeSpan) {
    final ArrayList<String> result = new ArrayList<String>();
    long time = timeSpan.startTime;
    while (time < timeSpan.endTime + Time.DAY_IN_MS) {
      result.add(Time.format(WinstonDatabase.WINSTON_TABLE_DATE_FORMAT, new Date(time)));
      time += Time.DAY_IN_MS;
    }
    return result;
  }

  /**
   * Finds data gaps in a given channel between two times. Returns null
   * on a Winston error. Returns a single item list with the given time span
   * if the channel doesn't exist or if no data exist in the interval.
   *
   * @param code
   * @param t1
   * @param t2
   * @return List of data gaps (each gap by a start and end time) or null if no data
   */
  public List<double[]> findGaps(final Scnl scnl, double t1, final double t2) {
    t1 = applyLookback(t1);
    if (t2 <= t1) {
      return null;
    }

    if (!winston.checkConnect())
      return null;

    String code = DbUtils.scnlAsWinstonCode(scnl);
    final List<double[]> gaps = new ArrayList<double[]>();
    if (!winston.useDatabase(code)) {
      // database didn't exist so the whole thing must be a gap
      gaps.add(new double[] {t1, t2});
      return gaps;
    }

    try {
      final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

      final List<String> days = daysBetween(t1, t2);
      final List<double[]> bufs = new ArrayList<double[]>(2 * ONE_DAY);

      for (final String day : days) {
        final double tst = J2kSec.parse(WinstonDatabase.WINSTON_TABLE_DATE_FORMAT, day);
        final double tet = tst + ONE_DAY;
        final String table = code + "$$" + day;
        if (!winston.tableExists(code, table))
          continue;
        if (tet < t1)
          continue;
        if (tst > t2)
          continue;

        final ResultSet rs = winston.getStatement()
            .executeQuery("SELECT st, et FROM `" + table + "` ORDER BY st ASC");
        while (rs.next()) {
          final double start = rs.getDouble(1);
          final double end = rs.getDouble(2);
          if (end < t1)
            continue;
          if (start > t2)
            continue;
          bufs.add(new double[] {start, end});
        }
        rs.close();
      }

      if (bufs.size() == 0) {
        // there were no tracebufs in the time range, the whole span is a gap.
        gaps.add(new double[] {t1, t2});
        return gaps;
      }

      if (bufs.get(0)[0] > t1) {
        gaps.add(new double[] {t1, bufs.get(0)[0]});
      }
      double last = bufs.get(0)[1];
      for (int i = 1; i < bufs.size(); i++) {
        final double[] buf = bufs.get(i);
        if (buf[0] - last > EPSILON) {
          double start = last;
          double end = buf[0];
          if (end < t1)
            continue;
          if (start > t2)
            continue;
          if (start < t1)
            start = t1;
          if (end > t2)
            end = t2;

          gaps.add(new double[] {start, end});
        }

        last = buf[1];
      }
      if (bufs.get(bufs.size() - 1)[1] < t2) {
        gaps.add(new double[] {bufs.get(bufs.size() - 1)[1], t2});
      }

      return gaps;
    } catch (ParseException e) {
      LOGGER.error("Cannot parse j2kSec from date");
    } catch (SQLException e) {
      LOGGER.error("Cannot get times");
    }
    return null;
  }


  public List<TimeSpan> findGaps(String code, TimeSpan timeSpan) {
    final List<TimeSpan> gaps = new ArrayList<TimeSpan>();
    timeSpan = new TimeSpan(applyLookback(timeSpan.startTime), timeSpan.endTime);

    if (timeSpan.startTime >= timeSpan.endTime) {
      LOGGER.debug("null timeSpan {}", timeSpan);
      return gaps;
    }

    if (!winston.checkConnect()) {
      LOGGER.debug("Cannot connect to winston");
      return gaps;
    }

    if (!winston.useDatabase(code)) {
      // database didn't exist so the whole thing must be a gap
      LOGGER.info("didn't find channel {}", code);
      gaps.add(timeSpan);
      return gaps;
    }

    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    final List<String> days = daysBetween(timeSpan);

    double startJ2k = J2kSec.fromEpoch(timeSpan.startTime);
    double endJ2k = J2kSec.fromEpoch(timeSpan.endTime);
    double last = startJ2k;
    for (final String day : days) {
      List<double[]> bufs;
      try {
        String table = code + "$$" + day;
        bufs = getBufTimes(code, table);
      } catch (SQLException e) {
        LOGGER.error("Unable to read day table {}:{}", code, day);
        bufs = new ArrayList<double[]>();
      }

      for (double[] buf : bufs) {
        if (startJ2k >= buf[1] || endJ2k <= buf[0]) {
          continue;
        }

        if (buf[0] - last > EPSILON) {
          gaps.add(new TimeSpan(J2kSec.asEpoch(last), J2kSec.asEpoch(buf[0])));
        }
        last = buf[1];
      }
    }
    
    if (last < endJ2k) {
      gaps.add(new TimeSpan(J2kSec.asEpoch(last), timeSpan.endTime));

    }

    return gaps;
  }

  private List<double[]> getBufTimes(String code, String table) throws SQLException {
    final List<double[]> bufs = new ArrayList<double[]>(2 * ONE_DAY);
    if (!winston.tableExists(code, table)) {
      return bufs;
    }

    String sql = String.format("SELECT st, et FROM `%s` ORDER BY st ASC", table);

    final ResultSet rs = winston.getStatement().executeQuery(sql);
    while (rs.next()) {
      final double start = rs.getDouble(1);
      final double end = rs.getDouble(2);
      bufs.add(new double[] {start, end});
    }
    rs.close();

    return bufs;

  }

  /**
   * Return wave data for timespan t1..t2 for channel w/ id sid; cap result at
   * maxrows size. Currently returns larger than asked for.
   *
   * @param sid
   *          id of channel
   * @param t1
   *          start time
   * @param t2
   *          end time
   * @param maxrows
   *          cap on size of result
   * @return wave
   * @throws UtilException
   */
  public Wave getWave(final int sid, final double t1, final double t2, final int maxrows)
      throws UtilException {
    final String code = channels.getChannelCode(sid);
    return getWave(code, t1, t2, maxrows);
  }

  /**
   * Yield tracebuf from a ResultSet as an array of buytes
   */
  private byte[] getTraceBufBytes(final ResultSet rs) throws SQLException, IOException {
    final Blob b = rs.getBlob("tracebuf");
    byte[] bs = b.getBytes(1, (int) b.length());
    bs = Zip.decompress(bs, 512);
    return bs;
  }

  /**
   * Modified 2006-08-14 to not check for consistent sample rates. That duty
   * is passed to TraceBuf.traceBufsToWave(). This should fix the HVO bug
   * that trig2disk can not get tracebufs with varying sample rates.
   *
   * @param code
   * @param t1
   * @param t2
   * @param maxrows
   * @return trace buf data or null if no data
   * @throws UtilException
   */
  public List<byte[]> getTraceBufBytes(final String code, double t1, final double t2,
      final int maxrows) throws UtilException {

    t1 = applyLookback(t1);
    if (t1 >= t2) {
      return null;
    }

    int numSamplesCounter = 0;
    if (!winston.checkConnect() || !winston.useDatabase(code))
      return null;
    try {
      double ct = t1;

      final ArrayList<byte[]> bufs = new ArrayList<byte[]>((int) Math.ceil(t2 - t1) + 1);
      final String endDate = dateFormat.format(J2kSec.asDate(t2));
      boolean done = false;
      ResultSet rs = null;

      // MySQL only uses one key for optimization so selecting tracebufs
      // that lie between st and et is prohibitively slow. By selecting
      // the latest 1 tracebuf where the st is less than the desired time
      // we can quickly find the tracebuf that may overlap into the
      // desired interval. Unfortunately, this causes a problem for the
      // edge case of selecting data right at the UTC day boundary. To
      // fix this the query must be run against the day of the st of the
      // desired interval plus the day before.
      final String[] initialDates = new String[] {dateFormat.format(J2kSec.asDate(t1 - ONE_DAY)),
          dateFormat.format(J2kSec.asDate(t1))};

      for (final String date : initialDates) {
        final String sql = "SELECT st, et, sr, datatype, tracebuf FROM `" + code + "$$" + date
            + "` WHERE st<? ORDER BY st DESC LIMIT 1";
        final PreparedStatement ps = winston.getPreparedStatement(sql);
        try {
          ps.setDouble(1, t1);
          rs = ps.executeQuery();
          if (rs.next()) {
            if (t1 >= rs.getDouble(1) && t1 <= rs.getDouble(2)) {
              final byte[] buf = getTraceBufBytes(rs);
              numSamplesCounter += getNumSamples(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3));
              if (maxrows > 0 && numSamplesCounter > maxrows) {
                throw new UtilException("Max rows (" + maxrows + " rows) "
                    + (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "")
                    + "exceeded.");
              }
              bufs.add(buf);
            }
          }
          rs.close();
        } catch (final SQLException e) {
          LOGGER.debug("No table found for {}, {}->{}", date, t1, t2);
        }
      }

      // got the first tracebuf, now lets get the rest.
      while (!done) {
        final String date = dateFormat.format(J2kSec.asDate(ct));
        if (date.equals(endDate))
          done = true;

        final String sql = "SELECT st, et, sr, datatype, tracebuf FROM `" + code + "$$" + date
            + "` WHERE st>=" + t1 + " AND st<=" + t2 + " ORDER BY st ASC";

        try {
          rs = winston.getStatement().executeQuery(sql);
        } catch (final SQLException e) {
          // table not found
          LOGGER.debug("No table found for {}$${})", code, date);
          ct += ONE_DAY;
          continue;
        }
        while (rs.next()) {

          final byte[] buf = getTraceBufBytes(rs);
          numSamplesCounter += getNumSamples(rs.getDouble(1), rs.getDouble(2), rs.getDouble(3));
          if (maxrows > 0 && numSamplesCounter > maxrows)
            throw new UtilException("Max rows (" + maxrows + " rows) "
                + (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "") + "exceeded.");
          bufs.add(buf);
        }
        rs.close();
        ct += ONE_DAY;
      }
      return bufs;
    } catch (final SQLException e) {
      LOGGER.debug("Could not get TraceBuf bytes for {}, {}->{}", code, t1, t2);
    } catch (final IOException e) {
      LOGGER.debug("Could not get TraceBuf bytes for {}, {}->{}", code, t1, t2);
    }
    return null;

  }

  /**
   * Return wave meta data for timespan t1..t2 for channel w/ code; cap result at
   * maxrows size.
   *
   * @param code
   *          channel
   * @param t1
   *          start time
   * @param t2
   *          end time
   * @param maxrows
   *          cap on size of result
   * @return wave
   */

  private int getNumSamples(final double st, final double et, final double sr) {
    return (int) (sr * (et - st));
  }

  public List<TraceBuf> getTraceBufs(final String code, final double t1, final double t2,
      final int maxrows) throws UtilException {
    final List<byte[]> rawBufs = getTraceBufBytes(code, t1, t2, maxrows);
    if (rawBufs == null || rawBufs.size() == 0)
      return null;

    try {
      final List<TraceBuf> traceBufs = new ArrayList<TraceBuf>(rawBufs.size());
      for (final byte[] buf : rawBufs)
        traceBufs.add(new TraceBuf(buf));

      return traceBufs;
    } catch (final IOException e) {
      LOGGER.error("Could not get TraceBufs for {}, {}->{}", code, t1, t2);
    }
    return null;
  }

  /**
   * Return wave data for timespan t1..t2 for channel w/ code; cap result at
   * maxrows size. Currently returns larger than asked for.
   *
   * @param code
   *          channel
   * @param t1
   *          start time
   * @param t2
   *          end time
   * @return wave
   */
  public Wave getWave(final String code, final double t1, final double t2, final int maxrows)
      throws UtilException {
    if (!winston.checkConnect() || !winston.useDatabase(code))
      return null;

    final List<TraceBuf> bufs = getTraceBufs(code, t1, t2, maxrows);
    if (bufs == null || bufs.size() == 0)
      return null;
    else {
      final Wave wave = TraceBuf.traceBufToWave(bufs);
      wave.convertToJ2K();
      return wave;
    }
  }

  public HelicorderData getHelicorderData(final Scnl scnl, double t1, final double t2,
      final int maxrows) throws UtilException {

    t1 = applyLookback(t1);
    if (t1 >= t2) {
      return null;
    }

    String code = DbUtils.scnlAsWinstonCode(scnl);

    if (!winston.checkConnect() || !winston.useDatabase(code))
      return null;
    try {
      double ct = t1;
      // this 'fixes' problems when a start time of 0000 UTC is asked for
      // and that data are actually stored in the previous day. Some
      // issues remain.
      final String endDate = dateFormat.format(J2kSec.asDate(t2));
      boolean done = false;
      final ArrayList<double[]> list = new ArrayList<double[]>((int) (t2 - t1) + 2);
      while (!done) {
        final String date = dateFormat.format(J2kSec.asDate(ct));
        if (date.equals(endDate))
          done = true;
        ct += ONE_DAY;
        final String table = code + "$$H" + date;
        ResultSet rs = null;
        String sql = "SELECT j2ksec, smin, smax, rcnt FROM `" + table
            + "` WHERE j2ksec>=? AND j2ksec<=? ORDER BY j2ksec ASC";

        if (maxrows != 0) {
          sql += " LIMIT " + (maxrows + 1);

          // Check the row count for the query before running the entire thing. Can save significant
          // time
          // for sufficiently large queries that exceed the maxrows parameter
          final PreparedStatement s = winston.getPreparedStatement(
              "SELECT COUNT(*) FROM (SELECT 1 " + sql.substring(sql.indexOf("FROM")) + ") as T");
          try {
            s.setDouble(1, t1);
            s.setDouble(2, t2);
            rs = s.executeQuery();
          } catch (final Exception e) {
            // table not found
            continue;
          }
          if (rs.next() && rs.getInt(1) > maxrows)
            throw new UtilException("Max rows (" + maxrows + " rows) "
                + (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "") + "exceeded.");
        }

        final PreparedStatement select = winston.getPreparedStatement(sql);
        try {
          select.setDouble(1, t1);
          select.setDouble(2, t2);
          rs = select.executeQuery();
        } catch (final Exception e) {
          // table not found
          continue;
        }

        while (rs.next()) {
          final double[] d = new double[] {rs.getDouble(1), rs.getDouble(2), rs.getDouble(3)}; // ,
                                                                                               // rs.getInt(4)
          // };
          list.add(d);
        }
        rs.close();
      }
      return new HelicorderData(list);
    } catch (final SQLException e) {
      LOGGER.error("Could not get helicorder for {}, {}->{}", code, t1, t2);
    }
    return null;
  }

  public RSAMData getRSAMData(final Scnl scnl, double t1, final double t2,
      final int maxrows, final DownsamplingType ds, final int dsInt) throws UtilException {

    t1 = applyLookback(t1);
    if (t1 >= t2) {
      return null;
    }

    if (!winston.checkConnect() || !winston.useDatabase(DbUtils.scnlAsWinstonCode(scnl)))
      return null;

    try {
      double ct = t1;
      // this 'fixes' problems when a start time of 0000 UTC is asked for
      // and that data are actually stored in the previous day. Some
      // issues remain.
      final String endDate = dateFormat.format(J2kSec.asDate(t2));
      boolean done = false;
      int numSamplesCounter = 0;
      final ArrayList<double[]> list = new ArrayList<double[]>((int) (t2 - t1) + 2);
      while (!done) {
        final String date = dateFormat.format(J2kSec.asDate(ct));
        if (date.equals(endDate))
          done = true;
        ct += ONE_DAY;
        final String table = DbUtils.scnlAsWinstonCode(scnl) + "$$H" + date;
        ResultSet rs = null;
        String sql = "SELECT j2ksec, rsam" + " FROM `" + table + "` WHERE j2ksec>=" + t1
            + " AND j2ksec<=" + t2 + " AND rcnt>0" + " ORDER BY j2ksec";
        try {
          sql = getDownsamplingSQL(sql, t1, ds, dsInt);
        } catch (final UtilException e) {
          throw new UtilException("Can't downsample dataset: " + e.getMessage());
        }
        if (maxrows != 0) {
          sql += " LIMIT " + (maxrows + 1);

          // If the dataset has a maxrows paramater, check that the number of requested rows doesn't
          // exceed that number prior to running the full query. This can save a decent amount of
          // time
          // for large queries. Note that this only applies for non-downsampled queries. This is
          // done for
          // two reasons: 1) If the user is downsampling, they already know they're dealing with a
          // lot of data
          // and 2) the way MySQL handles the multiple nested queries that would result makes it
          // slower than
          // just doing the full query to begin with.
          if (ds.equals(DownsamplingType.NONE)) {
            try {
              rs = winston.getStatement().executeQuery("SELECT COUNT(*) FROM (SELECT 1 "
                  + sql.substring(sql.indexOf("FROM")) + ") as T");
            } catch (final SQLException e) {
              // table not found
              continue;
            }
            if (rs.next() && rs.getInt(1) > maxrows) {
              throw new UtilException("Max rows (" + maxrows + " rows) "
                  + (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "")
                  + "exceeded.");
            }
          }
        }

        try {
          rs = winston.getStatement().executeQuery(sql);
        } catch (final Exception e) {
          // table not found
          continue;
        }

        // Check for the amount of data returned in a downsampled query. Non-downsampled queries are
        // checked above.
        if (!ds.equals(DownsamplingType.NONE) && maxrows != 0) {
          numSamplesCounter += getResultSetSize(rs);
          if (numSamplesCounter > maxrows) {
            throw new UtilException("Max rows (" + maxrows + " rows) "
                + (vdxName.length() > 0 ? ("for data source " + vdxName + " ") : "") + "exceeded.");
          }
        }
        while (rs.next()) {
          final double[] d = new double[] {rs.getDouble(1), rs.getDouble(2)};
          list.add(d);
        }
        rs.close();
      }
      return new RSAMData(list);
    } catch (final SQLException e) {
      LOGGER.error("Could not get RSAM for {}, {}->{}", DbUtils.scnlAsWinstonCode(scnl), t1, t2);
    }
    return null;
  }

  /**
   * Version of SQLDataSource.getDownsamplingSQL which doesn't use prepared statements, so as it's
   * not supported by
   * Winston.
   */
  private static String getDownsamplingSQL(final String sql, final double startTime,
      final DownsamplingType ds, final int dsInt) throws UtilException {
    if (!ds.equals(DownsamplingType.NONE) && dsInt <= 1)
      throw new UtilException("Downsampling interval should be more than 1");
    if (ds.equals(DownsamplingType.NONE))
      return sql;
    else if (ds.equals(DownsamplingType.DECIMATE))
      return "SELECT * FROM(SELECT fullQuery.*, @row := @row+1 AS rownum FROM (" + sql
          + ") fullQuery, (SELECT @row:=0) r) ranked WHERE rownum % " + dsInt + " = 1";
    else if (ds.equals(DownsamplingType.MEAN)) {
      final String sql_select_clause = sql.substring(6, sql.toUpperCase().indexOf("FROM") - 1);
      final String sql_from_where_clause = sql.substring(sql.toUpperCase().indexOf("FROM") - 1,
          sql.toUpperCase().lastIndexOf("ORDER BY") - 1);
      final String[] columns = sql_select_clause.split(",");
      StringBuffer sb = new StringBuffer();
      sb.append("SELECT ");
      for (final String column : columns) {
        sb.append("AVG(").append(column.trim()).append("), ");
      }
      sb.append("((j2ksec-").append(startTime).append(") DIV ").append(dsInt).append(") intNum ");
      sb.append(sql_from_where_clause);
      sb.append(" GROUP BY intNum");

      return sb.toString();
    } else
      throw new UtilException("Unknown downsampling type: " + ds);
  }

  public void setVdxName(final String name) {
    this.vdxName = name;
  }

  public static int getResultSetSize(final ResultSet rs) throws SQLException {
    if (rs == null) {
      return 0;
    }

    final int currentRow = rs.getRow();
    rs.beforeFirst();
    rs.last();
    final int size = rs.getRow();
    if (currentRow == 0) {
      rs.beforeFirst();
    } else {
      rs.absolute(currentRow);
    }
    return size;
  }

  private double applyLookback(double time) {
    double lookback = J2kSec.now() - winston.maxDays * Time.DAY_IN_S;
    return Math.max(time, lookback);
  }

  private long applyLookback(long time) {
    long lookback = CurrentTime.getInstance().now() - (winston.maxDays * Time.DAY_IN_MS);
    return Math.max(time, lookback);
  }

}
