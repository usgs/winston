package gov.usgs.volcanoes.winston.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;

/**
 * A class for manipulating data from Winston for use in the WWS.
 *
 * TODO: refactor. This is getting pretty messy.
 *
 * TODO: either implement or eliminate embargo and span.
 *
 * @author Dan Cervelli
 */
public class WaveServerEmulator {
  private static final Logger LOGGER = LoggerFactory.getLogger(WaveServerEmulator.class);

  protected final static int ONE_HOUR = 60 * 60;
  protected final static int ONE_DAY = 24 * ONE_HOUR;

  private final Channels channels;
  private final Data data;
  private final DateFormat dateFormat;
  private final DecimalFormat decimalFormat;
  private final WinstonDatabase winston;

  public WaveServerEmulator(final WinstonDatabase w) {
    winston = w;
    channels = new Channels(w);
    data = new Data(w);
    dateFormat = new SimpleDateFormat("yyyy_MM_dd");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    decimalFormat = (DecimalFormat) NumberFormat.getInstance();
    decimalFormat.setMaximumFractionDigits(200);
    decimalFormat.setGroupingUsed(false);
  }

  public int getChannelID(final String s, final String c, final String n) {
    return getChannelID(s, c, n, null);
  }

  public int getChannelID(final String s, final String c, final String n, final String l) {
    String loc = "";
    if (l != null && !l.equals("--")) {
      loc = "$" + l;
    }
    final String trueCode = s + "$" + c + "$" + n + loc;
    final int id = channels.getChannelID(trueCode);
    return id;
  }


  /**
   * TODO: implement embargo
   * TODO: implement span
   * TODO: make more efficient
   * TODO: return correct dataType
   *
   * @param embargo
   * @param span
   * @return menu
   */
  public List<String> getWaveServerMenu(final boolean scnl, final double embargo,
      final double span) {
    return getWaveServerMenu(scnl, embargo, span, 0);
  }

  public List<String> getWaveServerMenu(final boolean scnl, final double embargo, final double span,
      final double maxDays) {
    if (!winston.checkConnect()) {
      return null;
    }

    final List<Channel> sts = channels.getChannels();
    if (sts == null) {
      return null;
    }

    final List<String> list = new ArrayList<String>(sts.size());
    for (final Channel st : sts) {
      final String[] ss = st.getCode().split("\\$");
      final double[] ts = {st.getMinTime(), st.getMaxTime()};


      if (maxDays > 0) {
        ts[0] = Math.max(ts[0], J2kSec.now() - (maxDays * ONE_DAY));
      }

      if (ts != null && ts[0] < ts[1]) {

        if (!scnl && ss.length == 3) {
          list.add(" " + st.getSID() + " " + ss[0] + " " + ss[1] + " " + ss[2] + " "
              + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[0]))) + " "
              + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[1]))) + " s4 ");
        } else if (scnl) {
          final String loc = (ss.length == 4 ? ss[3] : "--");
          final String line = " " + st.getSID() + " " + ss[0] + " " + ss[1] + " " + ss[2] + " "
              + loc + " " + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[0]))) + " "
              + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[1]))) + " s4 ";
          list.add(line);
        }
      }
    }
    return list;
  }

  public String getWaveServerMenuItem(final int p, final double embargo, final double span) {
    if (!winston.checkConnect()) {
      return null;
    }

    try {
      String result = null;
      winston.useRootDatabase();
      final ResultSet rs =
          winston.getStatement().executeQuery("SELECT code FROM channels WHERE sid=" + p);
      if (rs.next()) {
        final String code = rs.getString(1);
        final double[] ts = data.getTimeSpan(code);

        /*
         * if (embargo != 0)
         * {
         * double now = CurrentTime.nowJ2K();
         * double emnow = now - embargo;
         * maxt = Math.min(emnow, maxt);
         * }
         * if (span != 0)
         * mint = Math.max(mint, maxt - span);
         */
        final StringTokenizer st = new StringTokenizer(code, "$");
        final String sta = st.nextToken();
        final String ch = st.nextToken();
        final String nw = st.nextToken();

        result = " " + p + " " + sta + " " + ch + " " + nw + " "
            + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[0]))) + " "
            + decimalFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ts[1]))) + " s4 ";
      }
      rs.close();
      return result;
    } catch (final Exception e) {
      LOGGER.error("Could not get wave server menu item. ({})", e.getLocalizedMessage());
    }
    return null;
  }

  public String getWaveServerMenuItem(final String s, final String c, final String n,
      final String l, final double embargo, final double span) {
    String loc = "";
    if (l != null && !l.equals("--")) {
      loc = "$" + l;
    }
    final String trueCode = s + "$" + c + "$" + n + loc;
    final int id = channels.getChannelID(trueCode);
    if (id == -1) {
      return null;
    } else {
      return getWaveServerMenuItem(id, embargo, span);
    }
  }

  public Object[] getWaveServerRaw(final String s, final String c, final String n, final double t1,
      final double t2) {
    return getWaveServerRaw(s, c, n, null, t1, t2);
  }

  // TODO: fix. Returning Object[] is not the right design
  public Object[] getWaveServerRaw(final String s, final String c, final String n, final String l,
      final double t1, final double t2) {
    String lc = "";
    if (l != null && !l.equals("--")) {
      lc = "$" + l;
    }
    final String code = s + "$" + c + "$" + n + lc;
    if (!winston.checkConnect() || !winston.useDatabase(code)) {
      return null;
    }
    List<byte[]> bufs = null;
    try {
      bufs = data.getTraceBufBytes(code, t1, t2, 0);
    } catch (final UtilException e) {
    }
    if (bufs == null || bufs.size() == 0) {
      return null;
    }

    try {
      final int sid = channels.getChannelID(code);
      final TraceBuf tb0 = new TraceBuf(bufs.get(0));
      final TraceBuf tbN = new TraceBuf(bufs.get(bufs.size() - 1));
      int total = 0;
      for (final byte[] buf : bufs) {
        total += buf.length;
      }

      String lr = "";
      if (l != null) {
        lr = " " + l;
      }
      final String hdr = sid + " " + s + " " + c + " " + n + lr + " F " + tb0.dataType() + " "
          + tb0.getStartTime() + " " + tbN.getEndTime() + " " + total;
      return new Object[] {hdr, new Integer(total), bufs};
    } catch (final Exception e) {
      LOGGER.error("Could not get raw wave.", e.getLocalizedMessage());
    }
    return null;
  }
}
