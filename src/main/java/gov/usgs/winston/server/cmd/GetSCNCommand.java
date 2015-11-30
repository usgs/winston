package gov.usgs.winston.server.cmd;

import gov.usgs.net.NetTools;
import gov.usgs.plot.data.Wave;
import gov.usgs.util.Util;
import gov.usgs.util.UtilException;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.WWSCommandString;

import java.nio.channels.SocketChannel;
import java.util.logging.Level;

/**
 *
 * @author Dan Cervelli
 */
public class GetSCNCommand extends BaseCommand
{

	public GetSCNCommand(NetTools nt, WinstonDatabase db, WWS wws)
	{
		super(nt, db, wws);
	}
	
	public void doCommand(Object info, SocketChannel channel)
	{
		int totalBytes = 0;
		WWSCommandString cmd = new WWSCommandString((String)info);
		if (!cmd.isLegalSCNTT(8))
			return;
		
		int sid = emulator.getChannelID(cmd.getS(), cmd.getC(), cmd.getN());
		if (sid == -1)
		{
			sendNoChannelResponse(cmd.getID(), 0, cmd.getS(), cmd.getC(), cmd.getN(), null, channel);
			return;
		}

		double t1 = Util.ewToJ2K(cmd.getT1(false));
		t1 = timeOrMaxDays(t1);
		
		double t2 = Util.ewToJ2K(cmd.getT2(false));
		t2 = timeOrMaxDays(t2);
		
		double[] bounds = checkTimes(sid, t1, t2);
		if (!allowTransaction(bounds))
		{
			netTools.writeString(cmd.getEarthwormErrorString(sid, getError(bounds)), channel);
			return;
		}
		Wave wave = null;
		try{
			wave = data.getWave(sid, t1, t2, 0);
		} catch (UtilException e){
		}
		if (wave != null)
			totalBytes = writeWaveAsAscii(wave, sid, cmd.getID(), cmd.getS(), cmd.getC(), cmd.getN(), null, t1, t2, cmd.getString(7), channel);
		else
			netTools.writeString(cmd.getEarthwormErrorString(sid, "FG s4"), channel);

		String scn = cmd.getS() + "_" + cmd.getC() + "_" + cmd.getN();
		String time = Util.j2KToDateString(t1) + " - " + Util.j2KToDateString(t2);
		wws.log(Level.FINER, "GETSCN " + scn + " : " + time + ", " + totalBytes + " bytes.", channel);
	}
}
