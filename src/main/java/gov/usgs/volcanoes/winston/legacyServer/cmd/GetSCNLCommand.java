package gov.usgs.volcanoes.winston.legacyServer.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;

import gov.usgs.net.NetTools;
import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;

/**
 *
 * @author Dan Cervelli
 */
public class GetSCNLCommand extends BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetSCNLCommand.class);

  public GetSCNLCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  public void doCommand(final Object info, final SocketChannel channel) {
    int totalBytes = 0;
    final WWSCommandString cmd = new WWSCommandString((String) info);
    if (!cmd.isLegalSCNLTT(9))
      return;

    final int sid = emulator.getChannelID(cmd.getS(), cmd.getC(), cmd.getN(), cmd.getL());
    if (sid == -1) {
      sendNoChannelResponse(cmd.getID(), 0, cmd.getS(), cmd.getC(), cmd.getN(), cmd.getL(),
          channel);
      return;
    }

    double t1 = J2kSec.fromEpoch(Ew.asEpoch(cmd.getT1(true)));
    t1 = timeOrMaxDays(t1);

    double t2 = J2kSec.fromEpoch(Ew.asEpoch(cmd.getT2(true)));
    t2 = timeOrMaxDays(t2);

    final double[] bounds = checkTimes(sid, t1, t2);
    if (!allowTransaction(bounds)) {
      netTools.writeString(cmd.getEarthwormErrorString(sid, getError(bounds)), channel);
      return;
    }
    Wave wave = null;
    try {
      wave = data.getWave(sid, t1, t2, 0);
    } catch (final UtilException e) {
    }
    if (wave != null)
      totalBytes = writeWaveAsAscii(wave, sid, cmd.getID(), cmd.getS(), cmd.getC(), cmd.getN(),
          cmd.getL(), t1, t2, cmd.getString(8), channel);
    else
      netTools.writeString(cmd.getEarthwormErrorString(sid, "FG s4"), channel);

    final String scn = cmd.getS() + "_" + cmd.getC() + "_" + cmd.getN() + "_" + cmd.getL();
    final String time = J2kSec.toDateString(t1) + " - " + J2kSec.toDateString(t2);
    LOGGER.debug("GETSCNL {} : {}, {} bytes.", scn, time, totalBytes);
  }
}
