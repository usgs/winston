package gov.usgs.winston.server.cmd;

import gov.usgs.math.DownsamplingType;
import gov.usgs.net.NetTools;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.WWSCommandString;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
 *
 * @author Dan Cervelli
 */
public class GetSCNLRSAMRawCommand extends BaseCommand
{
	public GetSCNLRSAMRawCommand(NetTools nt, WinstonDatabase db, WWS wws)
	{
		super(nt, db, wws);
	}
	
	public void doCommand(Object info, SocketChannel channel)
	{
		WWSCommandString cmd = new WWSCommandString((String)info);
		if (!cmd.isLegalSCNLTT(10) || Double.isNaN(cmd.getDouble(8)) || cmd.getInt(9) == Integer.MIN_VALUE)
			return;	 // malformed command

		RSAMData rsam = null;
		double t1 = Double.NaN;
		double t2 = Double.NaN;
		
		try{
			t1 = cmd.getT1(true);
			t1 = timeOrMaxDays(t1);
			
			t2 = cmd.getT2(true);
			t2 = timeOrMaxDays(t2);

			int ds = (int) cmd.getDouble(8);
			DownsamplingType dst = DownsamplingType.MEAN;
			if (ds < 2)
			    dst = DownsamplingType.NONE;
			
			rsam = data.getRSAMData(cmd.getWinstonSCNL(), t1, t2, 0, dst, ds);
		} catch (UtilException e){
		    // can I do anything here?
		}
		ByteBuffer bb = null;
		if (rsam != null && rsam.rows() > 0)
			bb = (ByteBuffer)rsam.toBinary().flip();
		int bytes = writeByteBuffer(cmd.getID(), bb, cmd.getInt(9) == 1, channel);
		
		String time = Util.j2KToDateString(t1) + " - " + Util.j2KToDateString(t2);
		wws.log(Level.FINER, "GETSCNLRSAMRAW " + cmd.getWinstonSCNL() + ": " + time + ", " + bytes + " bytes.", channel);
	}
}
