package gov.usgs.volcanoes.winston.server.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import gov.usgs.net.NetTools;
import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;
import gov.usgs.volcanoes.winston.server.WWSClient;
import gov.usgs.volcanoes.winston.server.WWSCommandString;

/**
 *
 * Example command string:
 * GETWAVERAW: GS AUI EHZ AV -- 284802563.447000 284802683.447000 1
 *
 * Compress wave before returning
 * Not supported by wave_serverV
 *
 * @author Dan Cervelli
 */
public class GetWaveRawCommand extends BaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetWaveRawCommand.class);

  public GetWaveRawCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  public void doCommand(final Object info, final SocketChannel channel) {
    final CodeTimer ct = new CodeTimer("GetWaveRaw");

    final WWSCommandString cmd = new WWSCommandString((String) info);
    if (!cmd.isLegalSCNLTT(9))
      return; // malformed command

    double et = cmd.getT2(true);
    et = timeOrMaxDays(et);

    double st = cmd.getT1(true);
    st = timeOrMaxDays(st);

    et = Math.min(et, J2kSec.now() - wws.getEmbargo());
    Wave wave = null;
    if (st < et) {
      try {
        wave = data.getWave(cmd.getWinstonSCNL(), st, et, 0);
      } catch (final UtilException e) {
      }
    }
    ct.stop();

    // Did it take too long to gather the data?
    if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
      LOGGER.info(
          String.format("slow db query (%1.2f ms) GETWAVERAW " + cmd.getWinstonSCNL() + " " + st
              + " -> " + et + " (" + decimalFormat.format(et - st) + ") ", ct.getRunTimeMillis()),
          channel);

    ByteBuffer bb = null;
    if (wave != null && wave.numSamples() > 0)
      bb = (ByteBuffer) wave.toBinary().flip();

    final boolean compress = cmd.getInt(8) == 1;

    ct.start();
    final int bytes = writeByteBuffer(cmd.getID(), bb, compress, channel);
    ct.stop();

    // Did it take too long to deliver the data?
    if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
      LOGGER.info(
          String.format("slow network (%1.2f ms) GETWAVERAW " + cmd.getWinstonSCNL() + " " + st
              + " -> " + et + " (" + decimalFormat.format(et - st) + ") ", ct.getRunTimeMillis()),
          channel);

    final String time = J2kSec.toDateString(st) + " - " + J2kSec.toDateString(et);
    LOGGER.info("GETWAVERAW {}: {}, {} bytes. ({})", cmd.getWinstonSCNL(), time, bytes, info);
  }
}
