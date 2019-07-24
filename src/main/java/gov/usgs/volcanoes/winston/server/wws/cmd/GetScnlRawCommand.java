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

import gov.usgs.volcanoes.core.contrib.HashCodeUtil;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.DbUtils;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.WinstonConsumer;
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

  protected Scnl scnl;
  protected TimeSpan timeSpan;
  protected int cmdHash;

  /**
   * Constructor.
   */
  public GetScnlRawCommand() {
    super();
  }

  protected void parseCommand(WwsCommandString cmd) throws MalformedCommandException {
    int hash = HashCodeUtil.hash(HashCodeUtil.SEED, cmd);
    if (cmdHash == Integer.MIN_VALUE || cmdHash != hash) {
      scnl = cmd.getScnl();
      timeSpan = cmd.getEwTimeSpan(WwsCommandString.HAS_LOCATION);
    }
  }

  protected String formatChannelName(Scnl scnl) {
    return scnl.toString(" ");
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    parseCommand(cmd);

    final String id = cmd.id;
    final String chan = formatChannelName(scnl);
    final String code = DbUtils.scnlAsWinstonCode(scnl);

    final double startTime = J2kSec.fromEpoch(timeSpan.startTime);
    final double endTime = J2kSec.fromEpoch(timeSpan.endTime);

    final Integer chanId = getChanId(scnl);
    if (chanId == -1) {
      LOGGER.info("Cannot find  {}", scnl);
      ctx.writeAndFlush(id + " " + id + " 0 " + chan + " FN\n");
      return;
    }

    final double[] timeSpan = getTimeSpan(chanId);

    String hdrPreamble = id + " " + chanId + " " + chan;
    String errorString = null;
    if (endTime < startTime) {
      errorString = hdrPreamble + " FB";
    } else if (endTime < timeSpan[0]) {
      errorString = hdrPreamble + " FL s4 " + Time.j2kToEw(timeSpan[0]);
    } else if (startTime > timeSpan[1]) {
      errorString = hdrPreamble + " FR s4 " + Time.j2kToEw(timeSpan[1]);
    }

    if (errorString != null) {
      LOGGER.debug("Returning error: {}", errorString);
      ctx.writeAndFlush(errorString + "\r\n");
      return;
    }

    final List<byte[]> bufs;
    try {
      bufs = databasePool.doCommand(new WinstonConsumer<List<byte[]>>() {
        public List<byte[]> execute(WinstonDatabase winston) throws UtilException {
          double st = Math.max(startTime, timeSpan[0]);
          double et = Math.min(endTime, timeSpan[1]);
          if (et < endTime) {
            LOGGER.debug("Trimming end time: " + J2kSec.toDateString(endTime) + " -> " + J2kSec.toDateString(et) + "\n");
          }
          return new Data(winston).getTraceBufBytes(code, st, et, 0);
        }
      });
    } catch (Exception e) {
      throw new UtilException("Unable to get chanId");
    }

    if (bufs == null || bufs.size() == 0) {
      ctx.writeAndFlush(hdrPreamble + " FG s4\n");
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

    String hdr = String.format("%s F %s %f %f %d", hdrPreamble, firstBuf.dataType(),
        firstBuf.getStartTime(), lastBuf.getEndTime(), total);
    ctx.writeAndFlush(hdr + "\n");
    LOGGER.debug("Returning header: {}", hdr);
    final ByteBuffer bb = ByteBuffer.allocate(total);
    for (final Iterator<byte[]> it = bufs.iterator(); it.hasNext();) {
      bb.put((byte[]) it.next());
    }
    bb.flip();
    LOGGER.debug("GETSCNLRAW returning {} bytes", bb.capacity());
    ctx.write(bb.array());
  }

  @Override
  protected String prettyRequest(WwsCommandString cmd) {
    try {
      parseCommand(cmd);
      return String.format("%s %s %s %s +%s", cmd.command, cmd.id, scnl,
          Time.toDateString(timeSpan.startTime), timeSpan.span());
    } catch (MalformedCommandException e) {
      return cmd.commandString;
    }
  }

}
