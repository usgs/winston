package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
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

  /**
   * Constructor.
   */
  public GetScnlCommand() {
    super();
//    isScnl = true;
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {
    
    final String id = cmd.id;
    final String chan = cmd.getScnl().toString(" ");
    final String code = cmd.getScnl().toString("$");

    final Integer chanId = getChanId(code);
    if (chanId == -1) {
      ctx.writeAndFlush(id + " " + id + " 0 " + chan + " FN\n");
      return;
    }

    TimeSpan ts = cmd.getJ2kSecTimeSpan();
    final double startTime = Time.ewToj2k(ts.startTime);
    final double endTime = Time.ewToj2k(ts.endTime);

    final double[] timeSpan = getTimeSpan(chanId);

    String hdrPreamble = id + " " + chanId + " " + chan + " ";
    String errorString = null;
    if (endTime < startTime) {
      errorString = hdrPreamble + "FB";
    } else if (endTime < timeSpan[0]) {
      errorString = hdrPreamble + "FL s4";
    } else if (startTime > timeSpan[1]) {
      errorString = hdrPreamble + "FR s4";
    }

    if (errorString != null) {
      ctx.writeAndFlush(errorString + "\n");
      return;
    }

    final Wave wave;
    try {
      wave = databasePool.doCommand(new WinstonConsumer<Wave>() {
        public Wave execute(WinstonDatabase winston) throws UtilException {
          double st = Math.max(startTime, timeSpan[0]);
          double et = Math.min(endTime, timeSpan[1]);
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

    final NumberFormat numberFormat = new DecimalFormat("#.######");
    String sts = null;

    // find first sample time
    double ct = wave.getStartTime() - wave.getRegistrationOffset();
    final double dt = 1 / wave.getSamplingRate();
    for (int i = 0; i < wave.numSamples(); i++) {
      if (ct >= (startTime - dt / 2))
        break;
      ct += dt;
    }
    sts = numberFormat.format(Ew.fromEpoch(J2kSec.asEpoch(ct)));
    final ByteBuffer bb = ByteBuffer.allocate(wave.numSamples() * 13 + 256);
    bb.put(id.getBytes());
    bb.put((byte) ' ');
    bb.put(Integer.toString(chanId).getBytes());
    bb.put(chan.getBytes());
    bb.put(" F s4 ".getBytes());
    bb.put(sts.getBytes());
    bb.put((byte) ' ');
    bb.put(Double.toString(wave.getSamplingRate()).getBytes());
    bb.put(" ".getBytes());
    int sample;
    ct = wave.getStartTime();
    // int samples = 0;
    for (int i = 0; i < wave.numSamples(); i++) {
      if (ct >= (startTime - dt / 2)) {
        // samples++;
        sample = wave.buffer[i];
        if (sample == Wave.NO_DATA)
          bb.put(cmd.args[1].getBytes());
        else
          bb.put(Integer.toString(wave.buffer[i]).getBytes());
        bb.put((byte) ' ');
      }
      ct += dt;
      if (ct >= endTime)
        break;
    }
    bb.put((byte) '\n');
    bb.flip();
    ctx.writeAndFlush(bb.array());
  }
}
