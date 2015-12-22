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
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetScnlHeliRawCommand extends WwsBaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetScnlHeliRawCommand.class);

  public GetScnlHeliRawCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {
    if (!cmd.isLegalSCNLTT(9)) {
      throw new MalformedCommandException();
    }

    final double et = cmd.getT2(true);
    final double st = cmd.getT1(true);
    final String scnl = cmd.getWinstonSCNL();
    
    if (et <= st) {
      throw new MalformedCommandException();
    }

    WinstonDatabase winston = null;
    HelicorderData heli;
    try {
      heli = databasePool.doCommand(new WinstonConsumer<HelicorderData>() {

        public HelicorderData execute(WinstonDatabase winston) throws UtilException {
          Data data = new Data(winston);
          return data.getHelicorderData(scnl, st, et, 0);
        }
      });
    } catch (Exception e1) {
      throw new UtilException(e1.getMessage());
    }

    ByteBuffer bb = null;
    if (heli != null && heli.rows() > 0) {
      bb = (ByteBuffer) heli.toBinary().flip();

      if (cmd.getInt(8) == 1)
        bb = ByteBuffer.wrap(Zip.compress(bb.array()));

      LOGGER.warn("returning {} heli bytes", bb.limit());
      ctx.write(cmd.getID() + " " + bb.limit() + "\n");
      ctx.writeAndFlush(bb.array());
    } else {
      LOGGER.warn("no heli data");
    }
  }
}
