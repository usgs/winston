/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.math.DownsamplingType;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.DbUtils;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * <cmd> = "GETSCNLRSAMRAW" <sp> <id> <sp> <scnl> <sp> <time span> <downsampling factor> 
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetScnlRsamRawCommand extends WwsBaseCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetScnlRsamRawCommand.class);
  protected Scnl scnl;
  protected TimeSpan timeSpan;
  
  /**
   * Constructor.
   */
  public GetScnlRsamRawCommand() {
    super();
  }

  private void parseCommand(WwsCommandString cmd) {
    
  }
  
  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    final String code = DbUtils.scnlAsWinstonCode(cmd.getScnl());

    TimeSpan ts = cmd.getJ2kSecTimeSpan();
    final double st = J2kSec.fromEpoch(ts.startTime);
    final double et = J2kSec.fromEpoch(ts.endTime);
    
    final int ds = cmd.getInt(-1);
    final DownsamplingType dst = (ds < 2) ? DownsamplingType.NONE : DownsamplingType.MEAN;

    RSAMData rsam;
    try {
      rsam = databasePool.doCommand(new WinstonConsumer<RSAMData>() {
        public RSAMData execute(WinstonDatabase winston) throws UtilException {
          return new Data(winston).getRSAMData(code, st, et, 0, dst, ds);
        }

      });
    } catch (Exception e) {
      throw new UtilException(e.getMessage());
    }

    ByteBuffer bb = null;
    if (rsam != null && rsam.rows() > 0)
      bb = (ByteBuffer) rsam.toBinary().flip();

    if (cmd.getInt(-1) == 1)
      bb = ByteBuffer.wrap(Zip.compress(bb.array()));

    if (bb != null) {
      LOGGER.debug("returning {} rsam bytes", bb.limit());
      ctx.write(cmd.id + " " + bb.limit() + '\n');
      ctx.writeAndFlush(bb.array());
    }
  }
}
