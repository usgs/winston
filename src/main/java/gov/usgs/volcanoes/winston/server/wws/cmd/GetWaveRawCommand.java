/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.Wave;
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
public class GetWaveRawCommand extends WwsBaseCommand {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(GetWaveRawCommand.class);

  /**
   * Constructor.
   */
  public GetWaveRawCommand() {
    super();
  }

  public void doCommand(final ChannelHandlerContext ctx, final WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    if (cmd.args.length < 6 || cmd.args.length > 7) {
      throw new MalformedCommandException(
          String.format("Wring number of arguments. (%s)", cmd.commandString));
    }

    final double et = cmd.getT2();
    final double st = cmd.getT1();
    if (st >= et) {
      throw new MalformedCommandException(
          String.format("End time must be after start time. (%s)", cmd.commandString));
    }

    Wave wave;
    try {
      wave = databasePool.doCommand(new WinstonConsumer<Wave>() {

        public Wave execute(WinstonDatabase winston) throws UtilException {
          Data data = new Data(winston);
          return data.getWave(cmd.scnl.toString("$"), st, et, 0);
        }
      });
    } catch (Exception e1) {
      throw new UtilException(e1.getMessage());
    }

    ByteBuffer bb = null;
    if (wave != null && wave.numSamples() > 0)
      bb = (ByteBuffer) wave.toBinary().flip();
    else
      bb = ByteBuffer.allocate(0);

    String id = cmd.id;

    if (cmd.getInt(-1) == 1)
      bb = ByteBuffer.wrap(Zip.compress(bb.array()));

    if (bb != null) {
      ctx.write(id + " " + bb.limit() + "\n");
      ctx.writeAndFlush(bb.array());
    } else {
      throw new UtilException("Unable to compress results.");
    }
  }
}
