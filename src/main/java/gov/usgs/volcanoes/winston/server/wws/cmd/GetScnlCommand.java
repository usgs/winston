package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.contrib.HashCodeUtil;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author Tom Parker
 * 
 * <cmd> = "GETSCNL" <sp> <id> <sp> <channel spec>
 *
 */
public class GetScnlCommand extends EwDataRequest {

  private static final Logger LOGGER = LoggerFactory.getLogger(GetScnlRawCommand.class);
  protected Scnl scnl;
  protected TimeSpan timeSpan;
  private byte[] fill;
  private int cmdHash = Integer.MIN_VALUE;

  /**
   * Constructor.
   */
  public GetScnlCommand() {
    super();
    // isScnl = true;
  }

  protected void parseCommand(WwsCommandString cmd) throws MalformedCommandException {
    int hash = HashCodeUtil.hash(HashCodeUtil.SEED, cmd);
    if (cmdHash == Integer.MIN_VALUE || cmdHash != hash) {
      scnl = cmd.getScnl();
      timeSpan = cmd.getEwTimeSpan(WwsCommandString.HAS_LOCATION);
      fill = cmd.args[1].getBytes();
    }
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    parseCommand(cmd);
    if (scnl == null || timeSpan == null) {
      throw new MalformedCommandException("Cannot parse command. " + cmd);
    }

    final Integer chanId = getChanId(scnl);
    if (chanId == -1) {
      ctx.writeAndFlush(String.format("%s FN%n", cmd.id));
      return;
    }

    final String chan = scnl.toString(" ");
    final double startTime = J2kSec.fromEpoch(timeSpan.startTime);
    final double endTime = J2kSec.fromEpoch(timeSpan.endTime);

    final double[] chanTimeSpan = getTimeSpan(chanId);

    String hdrPreamble = cmd.id + " " + chanId + " " + chan + " ";
    String errorString = null;
    if (endTime < startTime) {
      errorString = hdrPreamble + "FB";
    } else if (endTime < chanTimeSpan[0]) {
      errorString = hdrPreamble + "FL s4";
      LOGGER.debug("Request span too early. Req: {} - {}; Have: {} - {}",
          J2kSec.toDateString(startTime), J2kSec.toDateString(endTime),
          J2kSec.toDateString(chanTimeSpan[0]), J2kSec.toDateString(chanTimeSpan[1]));
    } else if (startTime > chanTimeSpan[1]) {
      errorString = hdrPreamble + "FR s4";
      LOGGER.debug("Request span too late. Req: {} - {}; Have: {} - {}",
          J2kSec.toDateString(startTime), J2kSec.toDateString(endTime),
          J2kSec.toDateString(chanTimeSpan[0]), J2kSec.toDateString(chanTimeSpan[1]));
    }

    if (errorString != null) {
      ctx.writeAndFlush(errorString + "\n");
      return;
    }

    final Wave wave;
    try {
      wave = databasePool.doCommand(new WinstonConsumer<Wave>() {
        public Wave execute(WinstonDatabase winston) throws UtilException {
          double st = Math.max(startTime, chanTimeSpan[0]);
          double et = Math.min(endTime, chanTimeSpan[1]);
          return new Data(winston).getWave(chanId, st, et, 0);
        }
      });
    } catch (Exception e) {
      throw new UtilException("Unable to get chanId");
    }


    if (wave == null) {
      ctx.writeAndFlush(hdrPreamble + "FG s4\n");
      LOGGER.debug("Returning empty trace list");
      return;
    }

    String header = String.format("%sF s4 %f %.1f ", hdrPreamble, Time.j2kToEw(wave.getStartTime()),
        wave.getSamplingRate());

    ctx.write(header);
    for (int i = 0; i < wave.numSamples(); i++) {
      int sample = wave.buffer[i];
      if (sample == Wave.NO_DATA) {
        ctx.write(fill);
      } else {
        ctx.write(Integer.toString(wave.buffer[i]));
      }
      ctx.writeAndFlush(" ");
    }
    ctx.writeAndFlush("\n");
//    bb.flip();
//    LOGGER.debug("GETSCNL returning {} bytes", bb.capacity());
//    ctx.writeAndFlush(bb.array());
}    

//    
//    
//    final ByteBuffer bb = ByteBuffer.allocate(wave.numSamples() * 13 + 256);
//    int sample;
//    for (int i = 0; i < wave.numSamples(); i++) {
//      sample = wave.buffer[i];
//      if (sample == Wave.NO_DATA) {
//        bb.put(fill);
//      } else {
//        bb.put(Integer.toString(wave.buffer[i]).getBytes());
//      }
//      bb.put((byte) ' ');
//    }
//    bb.put((byte) '\n');
//    bb.flip();
//    LOGGER.debug("GETSCNL returning {} bytes", bb.capacity());
//    ctx.writeAndFlush(bb.array());
//  }

  @Override
  protected String prettyRequest(WwsCommandString cmd) {
    if (scnl == null || timeSpan == null) {
      return cmd.commandString;
    } else {
      try {
        parseCommand(cmd);
        return String.format("%s %s %s %s +%s", cmd.command, cmd.id, scnl,
            Time.toDateString(timeSpan.startTime), timeSpan.span());
      } catch (MalformedCommandException e) {
        return cmd.commandString;
      }
    }
  }
}
