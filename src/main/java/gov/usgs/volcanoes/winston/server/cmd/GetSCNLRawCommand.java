package gov.usgs.volcanoes.winston.server.cmd;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import gov.usgs.net.NetTools;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;

/**
 * Winston implementation of earthworm wave_serverV GETSCNLRAW command
 *
 * @author Dan Cervelli
 */
public class GetSCNLRawCommand extends BaseCommand {
  public GetSCNLRawCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  public void doCommand(final Object info, final SocketChannel channel) {

    final CodeTimer ct = new CodeTimer("GetSCNLRaw");
    final String cmd = (String) info;

    final String[] ss = cmd.split(" ");
    if (ss.length < 8)
      return; // malformed command

    final String id = ss[1];
    final String s = ss[2];
    final String c = ss[3];
    final String n = ss[4];
    final String l = ss[5];
    double t1 = Double.NaN;
    double t2 = Double.NaN;
    try {
      t1 = Util.ewToJ2K(Double.parseDouble(ss[6]));
      t1 = timeOrMaxDays(t1);

      t2 = Util.ewToJ2K(Double.parseDouble(ss[7]));
      t2 = timeOrMaxDays(t2);
    } catch (final Exception e) {
    }

    if (id == null || s == null || c == null || n == null || Double.isNaN(t1) || Double.isNaN(t2))
      return; // malformed command

    final int sid = emulator.getChannelID(s, c, n, l);
    if (sid == -1) {
      sendNoChannelResponse(id, 0, s, c, n, l, channel);
      return;
    }

    final double[] bounds = checkTimes(sid, t1, t2);
    if (!allowTransaction(bounds)) {
      final String error =
          id + " " + sid + " " + s + " " + c + " " + n + " " + l + " " + getError(bounds) + "\n";
      netTools.writeString(error, channel);
      return;
    }

    final Object[] result = emulator.getWaveServerRaw(s, c, n, l, t1, t2);

    ct.stop();
    if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
      wws.log(Level.INFO,
          String.format(
              "slow db query (%1.2f ms) GETSCNLRAW " + s + "$" + c + "$" + n + "$" + l + " " + t1
                  + " -> " + t2 + " (" + decimalFormat.format(t2 - t1) + ") ",
              ct.getRunTimeMillis()),
          channel);

    int totalBytes = 0;
    if (result != null) {
      final String hdr = id + " " + (String) result[0] + "\n";
      final int bytes = ((Integer) result[1]).intValue();
      final List<?> items = (List<?>) result[2];
      final ByteBuffer bb = ByteBuffer.allocate(bytes);
      for (final Iterator<?> it = items.iterator(); it.hasNext();) {
        bb.put((byte[]) it.next());
      }
      bb.flip();

      ct.start();
      netTools.writeString(hdr, channel);
      totalBytes = netTools.writeByteBuffer(bb, channel);
      ct.stop();
      if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
        wws.log(Level.INFO,
            String.format(
                "slow network (%1.2f ms) GETSCNLRAW " + s + "$" + c + "$" + n + "$" + l + " " + t1
                    + " -> " + t2 + " (" + decimalFormat.format(t2 - t1) + ") ",
                ct.getRunTimeMillis()),
            channel);
    } else {
      // must be a gap
      netTools.writeString(id + " " + sid + " " + s + " " + c + " " + n + " " + l + " FG s4\n",
          channel);
    }

    final String scnl = s + "_" + c + "_" + n + "_" + l;
    final String time = Util.j2KToDateString(t1) + " - " + Util.j2KToDateString(t2);
    wws.log(Level.FINER, "GETSCNLRAW " + scnl + " : " + time + ", " + totalBytes + " bytes.",
        channel);
  }
}
