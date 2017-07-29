/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Answers requests using the earthworm WSV GETSCNLRAW command.
 * 
 * @author Tom Parker
 *
 */
public class GetScnlRawCommand extends EwDataRequest {

  private static final Logger LOGGER = LoggerFactory.getLogger(GetScnlRawCommand.class);

  /**
   * Constructor.
   */
  public GetScnlRawCommand() {
    super();
//    isScnl = true;
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {
    if (cmd.args.length < 2)
      throw new MalformedCommandException();

    final String id = cmd.getID();
    final String chan = cmd.scnl.toString(" ");
    final String code = cmd.scnl.toString("$");

    final double startTime = Time.ewToj2k(cmd.getT1());
    final double endTime = Time.ewToj2k(cmd.getT2());

    final Integer chanId = getChanId(code);
    if (chanId == -1) {
      ctx.writeAndFlush(id + " " + id + " 0 " + chan + " FN\n");
      return;
    }

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

    final List<byte[]> bufs;
    try {
      bufs = databasePool.doCommand(new WinstonConsumer<List<byte[]>>() {
        public List<byte[]> execute(WinstonDatabase winston) throws UtilException {
          double st = Math.max(startTime, timeSpan[0]);
          double et = Math.min(endTime, timeSpan[1]);
          return new Data(winston).getTraceBufBytes(code, st, et, 0);
        }
      });
    } catch (Exception e) {
      throw new UtilException("Unable to get chanId");
    }

    if (bufs == null || bufs.size() == 0) {
      ctx.writeAndFlush(hdrPreamble + "FG s4\n");
      LOGGER.debug("Returning empty trace list");
      return;
    }

    final TraceBuf firstBuf;
    final TraceBuf lastBuf;
    try {
      firstBuf = new TraceBuf(bufs.get(0));
      lastBuf = new TraceBuf(bufs.get(bufs.size() - 1));
    } catch (IOException e) {
      throw new UtilException("Unable to get bufs.");
    }

    int total = 0;
    for (final byte[] buf : bufs) {
      total += buf.length;
    }

    String hdr = hdrPreamble + " F " + firstBuf.dataType() + " " + firstBuf.getStartTime() + " "
        + lastBuf.getEndTime() + " " + total + '\n';
    ctx.write(hdr);

    final ByteBuffer bb = ByteBuffer.allocate(total);
    for (final Iterator<byte[]> it = bufs.iterator(); it.hasNext();) {
      bb.put((byte[]) it.next());
    }
    bb.flip();
    ctx.writeAndFlush(bb.array());
  }
}
