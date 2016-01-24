package gov.usgs.volcanoes.winston.legacyServer.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import gov.usgs.math.DownsamplingType;
import gov.usgs.net.NetTools;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;

/**
 *
 * @author Dan Cervelli
 */
public class GetSCNLRSAMRawCommand extends BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetSCNLRSAMRawCommand.class);

  public GetSCNLRSAMRawCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  public void doCommand(final Object info, final SocketChannel channel) {
    final WWSCommandString cmd = new WWSCommandString((String) info);
    if (!cmd.isLegalSCNLTT(10) || Double.isNaN(cmd.getDouble(8))
        || cmd.getInt(9) == Integer.MIN_VALUE)
      return; // malformed command

    RSAMData rsam = null;
    double t1 = Double.NaN;
    double t2 = Double.NaN;

    try {
      t1 = cmd.getT1(true);
      t1 = timeOrMaxDays(t1);

      t2 = cmd.getT2(true);
      t2 = timeOrMaxDays(t2);

      final int ds = (int) cmd.getDouble(8);
      DownsamplingType dst = DownsamplingType.MEAN;
      if (ds < 2)
        dst = DownsamplingType.NONE;

      rsam = data.getRSAMData(cmd.getWinstonSCNL(), t1, t2, 0, dst, ds);
    } catch (final UtilException e) {
      // can I do anything here?
    }
    ByteBuffer bb = null;
    if (rsam != null && rsam.rows() > 0)
      bb = (ByteBuffer) rsam.toBinary().flip();
    final int bytes = writeByteBuffer(cmd.getID(), bb, cmd.getInt(9) == 1, channel);

    final String time = J2kSec.toDateString(t1) + " - " + J2kSec.toDateString(t2);
    LOGGER.debug("GETSCNLRSAMRAW {}: {},{} bytes.", cmd.getWinstonSCNL(), time, bytes);
  }
}
