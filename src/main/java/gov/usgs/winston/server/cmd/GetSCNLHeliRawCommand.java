package gov.usgs.winston.server.cmd;

import gov.usgs.net.NetTools;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.WWSCommandString;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
 * Example command string:
 * GETSCNLHELIRAW: GS AUI EHZ AV -- 284714490.621000 284804490.621000 1
 *
 * @author Dan Cervelli
 */
public class GetSCNLHeliRawCommand extends BaseCommand
{
	public GetSCNLHeliRawCommand(NetTools nt, WinstonDatabase db, WWS wws)
	{
		super(nt, db, wws);
	}
	
	public void doCommand(Object info, SocketChannel channel)
	{
		WWSCommandString cmd = new WWSCommandString((String)info);
		if (!cmd.isLegalSCNLTT(9))
			return;	 // malformed command
		
		double et = cmd.getT2(true);
		et = timeOrMaxDays(et);
		
		double st = cmd.getT1(true);
		st = timeOrMaxDays(st);
		
		et = Math.min(et, CurrentTime.getInstance().nowJ2K() - wws.getEmbargo());
		HelicorderData heli = null;
		if (st < et){
			try{
				heli = data.getHelicorderData(cmd.getWinstonSCNL(), st, et, 0);
			} catch (UtilException e){
			}
		}
		ByteBuffer bb = null;
		if (heli != null && heli.rows() > 0)
			bb = (ByteBuffer)heli.toBinary().flip();
		boolean compress = cmd.getInt(8) == 1;
		int bytes = writeByteBuffer(cmd.getID(), bb, compress, channel);
		
		String time = Util.j2KToDateString(st) + " - " + Util.j2KToDateString(et);
		wws.log(Level.FINER, "GETSCNLHELIRAW " + cmd.getWinstonSCNL() + " : " + time + ", " + bytes + " bytes.", channel);
	}
}
