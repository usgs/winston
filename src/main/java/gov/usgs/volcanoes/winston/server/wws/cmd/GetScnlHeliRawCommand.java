/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.HelicorderData;
import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetScnlHeliRawCommand extends WwsBaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetScnlHeliRawCommand.class);

  /**
   * Constructor.
   */
  public GetScnlHeliRawCommand() {
    super();
  }

  public void doCommand(final ChannelHandlerContext ctx, final WwsCommandString cmd)
      throws MalformedCommandException, UtilException {
    if (!cmd.isLegalSCNLTT(9)) {
      throw new MalformedCommandException();
    }

    final double et = cmd.getT2();
    final double st = cmd.getT1();

    if (et <= st) {
      throw new MalformedCommandException();
    }

    HelicorderData heli;
    try {
      heli = databasePool.doCommand(new WinstonConsumer<HelicorderData>() {

        public HelicorderData execute(WinstonDatabase winston) throws UtilException {
          Data data = new Data(winston);
          return data.getHelicorderData(cmd.scnl.toString("$"), st, et, 0);
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

      LOGGER.debug("returning {} heli bytes", bb.limit());
      ctx.write(cmd.id + " " + bb.limit() + "\n");
      ctx.writeAndFlush(bb.array());
    } else {
      ctx.write(cmd.id + " 0\n");
    }
  }
}
