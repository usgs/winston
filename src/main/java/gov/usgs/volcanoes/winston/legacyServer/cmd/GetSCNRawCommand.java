package gov.usgs.volcanoes.winston.legacyServer.cmd;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSClient;

/**
 *
 * @author Dan Cervelli
 */
public class GetSCNRawCommand extends BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(WWSClient.class);

  public GetSCNRawCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  public void doCommand(final Object info, final SocketChannel channel) {
    final String cmd = (String) info;

    final String[] ss = cmd.split(" ");
    if (ss.length < 7)
      return; // malformed command

    final String id = ss[1];
    final String s = ss[2];
    final String c = ss[3];
    final String n = ss[4];
    double t1 = Double.NaN;
    double t2 = Double.NaN;
    try {
      t1 = J2kSec.fromEpoch(Ew.asEpoch(Double.parseDouble(ss[5])));
      t1 = timeOrMaxDays(t1);

      t2 = J2kSec.fromEpoch(Ew.asEpoch(Double.parseDouble(ss[6])));
      t2 = timeOrMaxDays(t2);
    } catch (final Exception e) {
    }

    if (id == null || s == null || c == null || n == null || Double.isNaN(t1) || Double.isNaN(t2))
      return; // malformed command

    final int sid = emulator.getChannelID(s, c, n);
    if (sid == -1) {
      sendNoChannelResponse(id, 0, s, c, n, null, channel);
      return;
    }

    final double[] bounds = checkTimes(sid, t1, t2);
    if (!allowTransaction(bounds)) {
      final String error =
          id + " " + sid + " " + s + " " + c + " " + n + " " + getError(bounds) + "\n";
      netTools.writeString(error, channel);
      return;
    }

    final Object[] result = emulator.getWaveServerRaw(s, c, n, t1, t2);
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

      netTools.writeString(hdr, channel);
      totalBytes = netTools.writeByteBuffer(bb, channel);
    } else {
      // must be a gap
      netTools.writeString(id + " " + sid + " " + s + " " + c + " " + n + " FG s4\n", channel);
    }
    final String scn = s + "_" + c + "_" + n;
    final String time = J2kSec.toDateString(t1) + " - " + J2kSec.toDateString(t2);
    LOGGER.debug("GETSCNRAW {} : {}, {} bytes.", scn, time, totalBytes);
  }
}
