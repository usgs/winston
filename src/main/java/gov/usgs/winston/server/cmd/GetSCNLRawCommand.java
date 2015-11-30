package gov.usgs.winston.server.cmd;

import gov.usgs.net.NetTools;
import gov.usgs.util.CodeTimer;
import gov.usgs.util.Util;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Winston implementation of earthworm wave_serverV GETSCNLRAW command
 * 
 * @author Dan Cervelli
 */
public class GetSCNLRawCommand extends BaseCommand {
	public GetSCNLRawCommand(NetTools nt, WinstonDatabase db, WWS wws) {
		super(nt, db, wws);
	}

	public void doCommand(Object info, SocketChannel channel) {

		CodeTimer ct = new CodeTimer("GetSCNLRaw");
		String cmd = (String) info;

		String[] ss = cmd.split(" ");
		if (ss.length < 8)
			return; // malformed command

		String id = ss[1];
		String s = ss[2];
		String c = ss[3];
		String n = ss[4];
		String l = ss[5];
		double t1 = Double.NaN;
		double t2 = Double.NaN;
		try {
			t1 = Util.ewToJ2K(Double.parseDouble(ss[6]));
			t1 = timeOrMaxDays(t1);

			t2 = Util.ewToJ2K(Double.parseDouble(ss[7]));
			t2 = timeOrMaxDays(t2);
		} catch (Exception e) {
		}

		if (id == null || s == null || c == null || n == null || Double.isNaN(t1) || Double.isNaN(t2))
			return; // malformed command

		int sid = emulator.getChannelID(s, c, n, l);
		if (sid == -1) {
			sendNoChannelResponse(id, 0, s, c, n, l, channel);
			return;
		}

		double[] bounds = checkTimes(sid, t1, t2);
		if (!allowTransaction(bounds)) {
			String error = id + " " + sid + " " + s + " " + c + " " + n + " " + l + " " + getError(bounds) + "\n";
			netTools.writeString(error, channel);
			return;
		}

		Object[] result = emulator.getWaveServerRaw(s, c, n, l, t1, t2);

		ct.stop();
		if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
			wws.log(Level.INFO,
					String.format("slow db query (%1.2f ms) GETSCNLRAW " + s + "$" + c + "$" + n + "$" + l + " " + t1
							+ " -> " + t2 + " (" + decimalFormat.format(t2 - t1) + ") ", ct.getRunTimeMillis()), channel);

		int totalBytes = 0;
		if (result != null) {
			String hdr = id + " " + (String) result[0] + "\n";
			int bytes = ((Integer) result[1]).intValue();
			List<?> items = (List<?>) result[2];
			ByteBuffer bb = ByteBuffer.allocate(bytes);
			for (Iterator<?> it = items.iterator(); it.hasNext();) {
				bb.put((byte[]) it.next());
			}
			bb.flip();

			ct.start();
			netTools.writeString(hdr, channel);
			totalBytes = netTools.writeByteBuffer(bb, channel);
			ct.stop();
			if (wws.getSlowCommandTime() > 0 && ct.getRunTimeMillis() > wws.getSlowCommandTime() * .75)
				wws.log(Level.INFO,
						String.format("slow network (%1.2f ms) GETSCNLRAW " + s + "$" + c + "$" + n + "$" + l + " "
								+ t1 + " -> " + t2 + " (" + decimalFormat.format(t2 - t1) + ") ", ct.getRunTimeMillis()), channel);
		} else {
			// must be a gap
			netTools.writeString(id + " " + sid + " " + s + " " + c + " " + n + " " + l + " FG s4\n", channel);
		}

		String scnl = s + "_" + c + "_" + n + "_" + l;
		String time = Util.j2KToDateString(t1) + " - " + Util.j2KToDateString(t2);
		wws.log(Level.FINER, "GETSCNLRAW " + scnl + " : " + time + ", " + totalBytes + " bytes.", channel);
	}
}