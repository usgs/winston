package gov.usgs.volcanoes.winston.legacyServer.cmd;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.net.NetTools;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.volcanoes.core.time.CurrentTime;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;

/**
 * Example command string:
 * GETSCNLHELIRAW: GS AUI EHZ AV -- 284714490.621000 284804490.621000 1
 *
 * @author Dan Cervelli
 */
public class GetSCNLHeliRawCommand extends BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetSCNLHeliRawCommand.class);

  public GetSCNLHeliRawCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  public void doCommand(final Object info, final SocketChannel channel) {
    final WWSCommandString cmd = new WWSCommandString((String) info);
    if (!cmd.isLegalSCNLTT(9))
      return; // malformed command

    double et = cmd.getT2(true);
    et = timeOrMaxDays(et);

    double st = cmd.getT1(true);
    st = timeOrMaxDays(st);

    et = Math.min(et, J2kSec.fromEpoch(CurrentTime.getInstance().now()) - wws.getEmbargo());
    HelicorderData heli = null;
    if (st < et) {
      try {
        heli = data.getHelicorderData(cmd.getWinstonSCNL(), st, et, 0);
      } catch (final UtilException e) {
      }
    }
    ByteBuffer bb = null;
    if (heli != null && heli.rows() > 0)
      bb = (ByteBuffer) heli.toBinary().flip();
    final boolean compress = cmd.getInt(8) == 1;
    final int bytes = writeByteBuffer(cmd.getID(), bb, compress, channel);

    final String time = J2kSec.toDateString(st) + " - " + J2kSec.toDateString(et);
    LOGGER.debug("GETSCNLHELIRAW {} : {}, {} bytes.", cmd.getWinstonSCNL(), time, bytes);
  }
}
