package gov.usgs.volcanoes.winston.server.wwsCmd;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.net.Command;
import gov.usgs.net.NetTools;
import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WaveServerEmulator;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.server.WinstonDatabasePool;
import io.netty.channel.ChannelHandlerContext;

/**
 *
 * @author Dan Cervelli
 */
abstract public class WwsBaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(WwsBaseCommand.class);

  protected final static int ONE_HOUR = 60 * 60;
  protected final static int ONE_DAY = 24 * ONE_HOUR;

  protected Data data;
  protected DecimalFormat decimalFormat;
  protected WaveServerEmulator emulator;
  protected int maxDays;
  protected NetTools netTools;
  protected WinstonDatabase winston;
  protected WWS wws;

  protected WinstonDatabasePool databasePool;

  public WwsBaseCommand() {
    decimalFormat = (DecimalFormat) NumberFormat.getInstance();
    decimalFormat.setMaximumFractionDigits(3);
    decimalFormat.setGroupingUsed(false);
  }

  // public WwsBaseCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
  // netTools = nt;
  // winston = db;
  // this.wws = wws;
  // maxDays = wws.getMaxDays();
  // emulator = new WaveServerEmulator(db);
  // data = new Data(db);
  // decimalFormat = (DecimalFormat) NumberFormat.getInstance();
  // decimalFormat.setMaximumFractionDigits(3);
  // decimalFormat.setGroupingUsed(false);
  // }

  protected boolean allowTransaction(final double[] d) {
    return !(d == null || d.length != 2 || Double.isNaN(d[0]) || Double.isNaN(d[1]));
  }

  protected double[] checkTimes(final int sid, double t1, double t2) {
    if (t1 >= t2) {
      return new double[] {Double.NaN, Double.NaN};
    }

    final double[] tb = data.getTimeSpan(sid);

    if (t1 < tb[0]) {
      t1 = tb[0];
    }

    // only apply the later bounds check if there is an embargo otherwise we
    // have to deal
    // with other people's idea of what now is
    if (t2 > tb[1]) {
      t2 = tb[1];
    }

    if (t2 < tb[0]) {
      return new double[] {Double.NaN, tb[0]};
    }

    if (t1 > tb[1]) {
      return new double[] {tb[1], Double.NaN};
    }

    return new double[] {t1, t2};
  }

  /**
   * Do the work. Return response to the browser.
   * 
   * @throws WwsMalformedCommand
   */
  public abstract void doCommand(ChannelHandlerContext ctx, WwsCommandString req)
      throws WwsMalformedCommand;

  protected String getError(final double[] d) {
    if (d == null || d.length != 2) {
      return "";
    }

    if (Double.isNaN(d[0]) && Double.isNaN(d[1])) {
      return "FB";
    }

    if (Double.isNaN(d[0])) {
      return "FL s4 " + Double.toString(Ew.fromEpoch(J2kSec.asEpoch(d[1])));
    }

    if (Double.isNaN(d[1])) {
      return "FR s4 " + Double.toString(Ew.fromEpoch(J2kSec.asEpoch(d[0])));
    }

    return "OK";
  }

  protected void sendNoChannelResponse(final String id, final int pin, final String s,
      final String c, final String n, final String l, final SocketChannel channel) {
    String loc = "";
    if (l != null) {
      loc = " " + l;
    }
    netTools.writeString(id + " " + id + " " + pin + " " + s + " " + c + " " + n + loc + " FN\n",
        channel);
  }

  /**
   * Apply maxDays to time
   *
   * @param t time
   * @return greater of t or now less maxDays
   */
  double timeOrMaxDays(final double t) {
    if (maxDays == 0) {
      return t;
    } else {
      return Math.max(t, J2kSec.now() - (maxDays * ONE_DAY));
    }
  }

  protected int writeByteBuffer(final String id, ByteBuffer bb, final boolean compress,
      final SocketChannel channel) {
    if (bb == null) {
      netTools.writeString(id + " 0\n", channel);
      return 0;
    }
    if (compress) {
      bb = ByteBuffer.wrap(Zip.compress(bb.array()));
    }

    netTools.writeString(id + " " + bb.limit() + "\n", channel);
    return netTools.writeByteBuffer(bb, channel);
  }

  public int writeWaveAsAscii(final Wave wave, final int sid, final String id, final String s,
      final String c, final String n, final String l, final double t1, final double t2,
      final String fill, final SocketChannel channel) {
    final NumberFormat numberFormat = new DecimalFormat("#.######");
    String sts = null;

    // find first sample time
    double ct = wave.getStartTime() - wave.getRegistrationOffset();
    final double dt = 1 / wave.getSamplingRate();
    for (int i = 0; i < wave.numSamples(); i++) {
      if (ct >= (t1 - dt / 2)) {
        break;
      }
      ct += dt;
    }
    sts = numberFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ct)));
    final ByteBuffer bb = ByteBuffer.allocate(wave.numSamples() * 13 + 256);
    bb.put(id.getBytes());
    bb.put((byte) ' ');
    bb.put(Integer.toString(sid).getBytes());
    bb.put((byte) ' ');
    bb.put(s.getBytes());
    bb.put((byte) ' ');
    bb.put(c.getBytes());
    bb.put((byte) ' ');
    bb.put(n.getBytes());
    if (l != null) {
      bb.put((byte) ' ');
      bb.put(l.getBytes());
    }
    bb.put(" F s4 ".getBytes());
    bb.put(sts.getBytes());
    bb.put((byte) ' ');
    bb.put(Double.toString(wave.getSamplingRate()).getBytes());
    bb.put(" ".getBytes());
    int sample;
    ct = wave.getStartTime();
    // int samples = 0;
    for (int i = 0; i < wave.numSamples(); i++) {
      if (ct >= (t1 - dt / 2)) {
        // samples++;
        sample = wave.buffer[i];
        if (sample == Wave.NO_DATA) {
          bb.put(fill.getBytes());
        } else {
          bb.put(Integer.toString(wave.buffer[i]).getBytes());
        }
        bb.put((byte) ' ');
      }
      ct += dt;
      if (ct >= t2) {
        break;
      }
    }
    bb.put((byte) '\n');
    bb.flip();
    return netTools.writeByteBuffer(bb, channel);
  }

  public void databasePool(WinstonDatabasePool databasePool) {
    this.databasePool = databasePool;
  }
}
