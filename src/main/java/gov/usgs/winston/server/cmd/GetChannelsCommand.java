package gov.usgs.winston.server.cmd;

import gov.usgs.net.NetTools;
import gov.usgs.winston.Channel;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.WWSCommandString;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.logging.Level;

/**
 * Example command string:
 * GETCHANNELS: GC METADATA
 *
 * @author Dan Cervelli
 */
public class GetChannelsCommand extends BaseCommand
{
	private Channels channels;
	
	public GetChannelsCommand(NetTools nt, WinstonDatabase db, WWS wws)
	{
		super(nt, db, wws);
		channels = new Channels(db);
	}
	
	public void doCommand(Object info, SocketChannel channel)
	{
		WWSCommandString cmd = new WWSCommandString((String)info);
		if (!cmd.isLegal(2) && !cmd.isLegal(3))
			return; // malformed command;
	
		boolean metadata = false;
		if (cmd.getString(2) != null && cmd.getString(2).equals("METADATA"))
			metadata = true;
		
		List<Channel> chs = channels.getChannels();
		StringBuilder sb = new StringBuilder(chs.size() * 60);
		sb.append(String.format("%s %d\n", cmd.getID(), chs.size()));
		for (Channel ch : chs)
		{
			if (metadata)
				sb.append(ch.toMetadataString(maxDays) + "\n");
			else
				sb.append(ch.toPV2String(maxDays) + "\n");
		}
		
		netTools.writeString(sb.toString(), channel);
		String c = "GETCHANNELS";
		if (metadata)
			c = c + " (METADATA)";
		wws.log(Level.FINER, c + ".", channel);
	}
}
