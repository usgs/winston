package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.dataselect;


import edu.iris.dmc.seedcodec.B1000Types;
import edu.iris.dmc.seedcodec.Steim2;
import edu.iris.dmc.seedcodec.SteimException;
import edu.iris.dmc.seedcodec.SteimFrameBlock;
import edu.sc.seis.seisFile.mseed.Blockette1000;
import edu.sc.seis.seisFile.mseed.Btime;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.net.HttpResponse;
import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.FdsnException;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.command.FdsnQueryCommand;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint.FdsnChannelConstraint;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint.FdsnTimeSimpleConstraint;

/**
 * TODO: convert to chunked encoding see:
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.6
 *
 * @author Tom Parker
 *
 */
public class FdsnDataselectQuery extends FdsnQueryCommand implements FdsnDataselectService {

  public FdsnDataselectQuery(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    version = VERSION;
  }

  @Override
  public String getCommand() {
    return "/fdsnws/dataselect/1/query";
  }

  @Override
  protected void sendResponse() {
    final HttpResponse response = new HttpResponse("text/plain");
    response.setLength("".length());
    netTools.writeString(response.getHeaderString(), socketChannel);

    OutputStream outputStream = null;
    try {
      outputStream = socketChannel.socket().getOutputStream();
      for (final Channel c : prunedChanList) {
        sendData(c, outputStream);
      }
    } catch (final IOException e) {
      sendError(500, "bad");
    } catch (final FdsnException e) {
      sendError(500, e.message);
    } finally {
      try {
        if (outputStream != null)
          outputStream.close();
      } catch (final IOException e) {
        // already closed
      }
    }
  }

  private void sendData(final Channel chan, final OutputStream outputStream)
      throws IOException, FdsnException {

    final DataOutputStream dos = new DataOutputStream(outputStream);
    int minLength;
    try {
      minLength = Integer.parseInt(arguments.get("minimumlength"));
    } catch (final NumberFormatException e) {
      minLength = -1;
    }

    // find matching channelConstraint
    FdsnChannelConstraint channelConstraint = null;
    for (final FdsnChannelConstraint c : channelConstraints) {
      channelConstraint = c;
      if (c.matches(chan))
        break;
    }
    if (channelConstraint == null)
      return;

    final FdsnTimeSimpleConstraint timeConstraint = channelConstraint.getTimeConstraint();
    if (timeConstraint == null)
      return;

    double start = timeConstraint.startTime;
    final List<TraceBuf> bufs = new ArrayList<TraceBuf>();
    int seq = 0;
    while (start < timeConstraint.endTime) {
      final double end = Math.min(start + ONE_HOUR, timeConstraint.endTime);
      try {
        bufs.addAll(data.getTraceBufs(chan.getCode(), start, end, 0));
      } catch (final UtilException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      final double previousEnd;
      final Iterator<TraceBuf> it = bufs.listIterator();

      while (it.hasNext()) {
        final TraceBuf tb = it.next();
        final DataHeader header = new DataHeader(seq++, 'D', false);
        header.setStationIdentifier(chan.station);
        header.setChannelIdentifier(chan.channel);
        header.setNetworkCode(chan.network);
        header.setLocationIdentifier(chan.location);

        header.setNumSamples((short) tb.bytes.length);
        header.setSampleRate(tb.samplingRate());
        final Btime btime = new Btime(Util.j2KToDate(tb.firstSampleTime()));
        header.setStartBtime(btime);

        final DataRecord record = new DataRecord(header);
        try {
          final Blockette1000 blockette1000 = new Blockette1000();
          blockette1000.setEncodingFormat((byte) B1000Types.STEIM2);
          blockette1000.setWordOrder((byte) 1);

          // record length as a power of 2
          blockette1000.setDataRecordLength((byte) (12));

          record.addBlockette(blockette1000);

          SteimFrameBlock data = null;

          data = Steim2.encode(tb.samples(), 63);

          record.setData(data.getEncodedData());
          record.write(dos);
        } catch (final SeedFormatException e) {
          e.printStackTrace();
        } catch (final SteimException e) {
          e.printStackTrace();
        }

      }
      start = end;
    }



  }
}
