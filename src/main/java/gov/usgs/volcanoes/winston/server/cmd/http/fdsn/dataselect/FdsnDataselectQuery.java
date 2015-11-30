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

    public FdsnDataselectQuery(NetTools nt, WinstonDatabase db, WWS wws) {
        super(nt, db, wws);
        version = VERSION;
    }

    public String getCommand() {
        return "/fdsnws/dataselect/1/query";
    }

    protected void sendResponse() {
        HttpResponse response = new HttpResponse("text/plain");
        response.setLength("".length());
        netTools.writeString(response.getHeaderString(), socketChannel);

        OutputStream outputStream = null;
        try {
            outputStream = socketChannel.socket().getOutputStream();
            for (Channel c : prunedChanList) {
                sendData(c, outputStream);
            }
        } catch (IOException e) {
            sendError(500, "bad");
        } catch (FdsnException e) {
            sendError(500, e.message);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                // already closed
            }
        }
    }

    private void sendData(Channel chan, OutputStream outputStream) throws IOException, FdsnException {

        DataOutputStream dos = new DataOutputStream(outputStream);
        int minLength;
        try {
            minLength = Integer.parseInt(arguments.get("minimumlength"));
        } catch (NumberFormatException e) {
            minLength = -1;
        }
        
        // find matching channelConstraint
        FdsnChannelConstraint channelConstraint = null;
        for (FdsnChannelConstraint c : channelConstraints) {
            channelConstraint = c;
            if (c.matches(chan))
                break;
        }
        if (channelConstraint == null)
            return;
        
        FdsnTimeSimpleConstraint timeConstraint = channelConstraint.getTimeConstraint();
        if (timeConstraint == null)
            return;
        
        double start = timeConstraint.startTime;
        List<TraceBuf>bufs = new ArrayList<TraceBuf>();
        int seq=0;
        while (start < timeConstraint.endTime) {
            double end = Math.min(start + ONE_HOUR, timeConstraint.endTime);
            try {
                bufs.addAll(data.getTraceBufs(chan.getCode(), start, end, 0));
            } catch (UtilException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            double previousEnd;
            Iterator<TraceBuf> it = bufs.listIterator();
            
            while (it.hasNext()) {
                TraceBuf tb = it.next();
                DataHeader header = new DataHeader(seq++, 'D', false);
                header.setStationIdentifier(chan.station);
                header.setChannelIdentifier(chan.channel);
                header.setNetworkCode(chan.network);
                header.setLocationIdentifier(chan.location);
                
                header.setNumSamples((short)tb.bytes.length);
                header.setSampleRate(tb.samplingRate());
                Btime btime = new Btime(Util.j2KToDate(tb.firstSampleTime()));
                header.setStartBtime(btime);
                
                DataRecord record = new DataRecord(header);
                try {
                    Blockette1000 blockette1000 = new Blockette1000();
                    blockette1000.setEncodingFormat((byte) B1000Types.STEIM2);
                    blockette1000.setWordOrder((byte) 1);

                    // record length as a power of 2
                    blockette1000.setDataRecordLength((byte) (12));

                    record.addBlockette(blockette1000);

                    SteimFrameBlock data = null;

                    data = Steim2.encode(tb.samples(), 63);

                    record.setData(data.getEncodedData());
                    record.write(dos);
                } catch (SeedFormatException e) {
                    e.printStackTrace();
                } catch (SteimException e) {
                    e.printStackTrace();
                }

            }
            start = end;
        }
        
        




    }
}
