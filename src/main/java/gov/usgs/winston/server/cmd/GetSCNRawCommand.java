package gov.usgs.winston.server.cmd;

import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author Dan Cervelli
 */
public class GetSCNRawCommand extends BaseCommand
{
	public GetSCNRawCommand(NetTools nt, WinstonDatabase db, WWS wws)
	{
		super(nt, db, wws);
	}
	
	public void doCommand(Object info, SocketChannel channel)
	{
		String cmd = (String)info;

		String[] ss = cmd.split(" ");
		if (ss.length < 7)
			return; // malformed command

		String id = ss[1];
		String s = ss[2];
		String c = ss[3];
		String n = ss[4];
		double t1 = Double.NaN;
		double t2 = Double.NaN;
		try
		{
			t1 = Util.ewToJ2K(Double.parseDouble(ss[5]));
			t1 = timeOrMaxDays(t1);
			
			t2 = Util.ewToJ2K(Double.parseDouble(ss[6]));
			t2 = timeOrMaxDays(t2);
		}
		catch (Exception e)
		{}

		if (id == null || s == null || c == null || n == null || Double.isNaN(t1) || Double.isNaN(t2))
			return; // malformed command

		int sid = emulator.getChannelID(s, c, n);
		if (sid == -1)
		{
			sendNoChannelResponse(id, 0, s, c, n, null, channel);
			return;
		}

		double[] bounds = checkTimes(sid, t1, t2);
		if (!allowTransaction(bounds))
		{
			String error = id + " " + sid + " " + s + " " + c + " " + n + " " + getError(bounds) + "\n";
			netTools.writeString(error, channel);
			return;
		}

		Object[] result = emulator.getWaveServerRaw(s, c, n, t1, t2);
		int totalBytes = 0;
		if (result != null)
		{
			String hdr = id + " " + (String)result[0] + "\n";
			int bytes = ((Integer)result[1]).intValue();
			List<?> items = (List<?>)result[2];
			ByteBuffer bb = ByteBuffer.allocate(bytes);
			for (Iterator<?> it = items.iterator(); it.hasNext();)
			{
				bb.put((byte[])it.next());
			}
			bb.flip();

			netTools.writeString(hdr, channel);
			totalBytes = netTools.writeByteBuffer(bb, channel);
		}
		else
		{
			// must be a gap
			netTools.writeString(id + " " + sid + " " + s + " " + c + " " + n + " FG s4\n", channel);
		}
		String scn = s + "_" + c + "_" + n;
		String time = Util.j2KToDateString(t1) + " - " + Util.j2KToDateString(t2);
		wws.log(Level.FINER, "GETSCNRAW " + scn + " : " + time + ", " + totalBytes + " bytes.", channel);
	}
}
