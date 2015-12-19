/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wwsCmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import gov.usgs.net.ConnectionStatistics;
import gov.usgs.net.NetTools;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.time.CurrentTime;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;
import gov.usgs.volcanoes.winston.legacyServer.cmd.BaseCommand;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetWaveRawCommand extends WwsBaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetWaveRawCommand.class);

  public GetWaveRawCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException {

    if (!cmd.isLegalSCNLTT(9))
      return; // malformed command

    double et = cmd.getT2(true);
    double st = cmd.getT1(true);

    if (st >= et) {
      throw new MalformedCommandException();
    }

    WinstonDatabase winston = null;
    Wave wave = null;
    try {
      winston = databasePool.borrowObject();
      if (!winston.checkConnect()) {
        LOGGER.error("WinstonDatabase unable to connect to MySQL.");
      } else {
        Data data = new Data(winston);
        wave = data.getWave(cmd.getWinstonSCNL(), st, et, 0);
      }
    } catch (Exception e) {
      LOGGER.error("Unable to fulfill command.", e);
    } finally {
      if (winston != null) {
        databasePool.returnObject(winston);
      }
    }

    ByteBuffer bb = null;
    if (wave != null && wave.numSamples() > 0)
      bb = (ByteBuffer) wave.toBinary().flip();

    final boolean compress = cmd.getInt(8) == 1;

    // final int bytes = writeByteBuffer(cmd.getID(), bb, compress, channel);

    String id = cmd.getID();

    if (cmd.getInt(8) == 1)
      bb = ByteBuffer.wrap(Zip.compress(bb.array()));

    LOGGER.warn("returning {} heli bytes", bb.limit());
    ctx.write(id + " " + bb.limit() + "\n");
    ctx.writeAndFlush(bb.array());
  }
}
