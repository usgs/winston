package gov.usgs.volcanoes.winston.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import gov.usgs.util.CodeTimer;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.winston.in.ew.ImportWSJob;

/**
 * A class for merging a table from a source Winston to a destination Winston.
 *
 * TODO: better parsing of arguments and adjustable log levels.
 *
 * @author Dan Cervelli
 */
public class Merge {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportWSJob.class);

  private static final double EPSILON = 0.0001;
  private WinstonDatabase source;
  private WinstonDatabase dest;

  public Merge(final String srcURL, final String destURL) {
    final String driver = "org.gjt.mm.mysql.Driver";
    source = new WinstonDatabase(driver, srcURL, null);
    LOGGER.info("Connected to source.");
    dest = new WinstonDatabase(driver, destURL, null);
    LOGGER.info("Connected to destination.");
  }

  public void flip() {
    final WinstonDatabase temp = source;
    source = dest;
    dest = temp;
  }

  public void mergeHelicorders(final String code, final String date) {
    try {
      LOGGER.info("Begin merging helicorders.");
      final Set<Integer> sourceTimes = new HashSet<Integer>();
      int total = 0;
      LOGGER.info("Getting source times.");
      source.useDatabase(code);
      ResultSet srs =
          source.getStatement().executeQuery("SELECT j2ksec FROM `" + code + "$$H" + date + "`");
      while (srs.next()) {
        total++;
        sourceTimes.add((int) Math.round(srs.getDouble(1)));
      }

      LOGGER.info("Getting destination times.");
      dest.useDatabase(code);
      final ResultSet drs =
          dest.getStatement().executeQuery("SELECT j2ksec FROM `" + code + "$$H" + date + "`");
      total = 0;
      while (drs.next()) {
        total++;
        sourceTimes.remove(new Integer((int) Math.round(drs.getDouble(1))));
      }

      total = 0;
      double read = 0;
      double write = 0;
      LOGGER.info("Begin merging.");
      final PreparedStatement insert = dest.getConnection()
          .prepareStatement("INSERT IGNORE INTO `" + code + "$$H" + date + "` VALUES (?,?,?,?,?)");
      final CodeTimer readTimer = new CodeTimer();
      for (final Iterator<Integer> it = sourceTimes.iterator(); it.hasNext();) {
        final int d = it.next().intValue();
        srs = source.getStatement().executeQuery("SELECT j2ksec, smin, smax, rcnt, rsam FROM `"
            + code + "$$H" + date + "` WHERE j2ksec=" + d);
        if (srs.next()) {
          insert.setDouble(1, srs.getDouble(1));
          insert.setInt(2, srs.getInt(2));
          insert.setInt(3, srs.getInt(3));
          insert.setInt(4, srs.getInt(4));
          insert.setDouble(5, srs.getDouble(5));
          final CodeTimer writeTimer = new CodeTimer();
          insert.execute();
          writeTimer.stopAndReport();
          write += writeTimer.getRunTimeMillis();
          total++;
        }
      }
      readTimer.stop();
      read = readTimer.getRunTimeMillis() - write;
      LOGGER.info("Done merging, " + read + "ms reading, " + write + "ms writing.");
      LOGGER.info("Merged " + total + " helicorder rows.");
    } catch (final Exception e) {
      LOGGER.error("Could not merge waves. {}", e);
    }
  }

  public void mergeWaves(final String code, final String date) {
    try {
      LOGGER.info("Begin merging waves.");
      final Set<Double> sourceTimes = new HashSet<Double>();
      int total = 0;
      LOGGER.info("Getting source times.");
      source.useDatabase(code);
      ResultSet srs =
          source.getStatement().executeQuery("SELECT st FROM `" + code + "$$" + date + "`");
      while (srs.next()) {
        total++;
        sourceTimes.add(Util.register(srs.getDouble(1), EPSILON));
      }

      LOGGER.info("Getting destination times.");
      dest.useDatabase(code);
      final ResultSet drs =
          dest.getStatement().executeQuery("SELECT st FROM `" + code + "$$" + date + "`");
      total = 0;
      while (drs.next()) {
        total++;
        sourceTimes.remove(new Double(Util.register(drs.getDouble(1), EPSILON)));
      }

      total = 0;
      LOGGER.info("Begin merging.");
      double read = 0;
      double write = 0;
      final PreparedStatement insert = dest.getConnection()
          .prepareStatement("INSERT IGNORE INTO `" + code + "$$" + date + "` VALUES (?,?,?,?,?)");
      final CodeTimer readTimer = new CodeTimer();
      for (final Iterator<Double> it = sourceTimes.iterator(); it.hasNext();) {
        final double d = it.next().doubleValue();
        srs = source.getStatement().executeQuery("SELECT st, et, sr, datatype, tracebuf FROM `"
            + code + "$$" + date + "` WHERE st>=" + (d - EPSILON) + " AND st<=" + (d + EPSILON));
        if (srs.next()) {
          insert.setDouble(1, srs.getDouble(1));
          insert.setDouble(2, srs.getDouble(2));
          insert.setDouble(3, srs.getDouble(3));
          insert.setString(4, srs.getString(4));
          insert.setBlob(5, srs.getBlob(5));
          final CodeTimer writeTimer = new CodeTimer();
          insert.execute();
          writeTimer.stopAndReport();
          write += writeTimer.getRunTimeMillis();
          total++;
        }
      }
      readTimer.stop();
      read = readTimer.getRunTimeMillis() - write;
      LOGGER.info("Done merging, " + read + "ms reading, " + write + "ms writing.");
      LOGGER.info("Merged " + total + " wave rows.");
    } catch (final Exception e) {
      LOGGER.error("Could not merge waves. {}", e);
    }
  }

  public void fullMerge(final String code, final String date) {
    mergeWaves(code, date);
    mergeHelicorders(code, date);
    flip();
    mergeWaves(code, date);
    mergeHelicorders(code, date);
  }

  public void merge(final String code, final String date) {
    mergeWaves(code, date);
    mergeHelicorders(code, date);
  }

  public static void main(final String[] args) {
    if (args.length != 4) {
      System.err.println("usage: java gov.usgs.volcanoes.winston.db.Merge [srcURL] [destURL] [table] [date]");
      System.err.println("[table] is case sensitive; example: CRP_SHZ_AK");
      System.err.println("[date] is in YYYY_MM_DD form; example: 2005_03_27");
      System.exit(1);
    }
    final Merge merge = new Merge(args[0], args[1]);
    merge.merge(args[2], args[3]);
  }
}
