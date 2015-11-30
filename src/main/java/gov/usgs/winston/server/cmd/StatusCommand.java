package gov.usgs.winston.server.cmd;

import gov.usgs.net.ConnectionStatistics;
import gov.usgs.net.Connections;
import gov.usgs.net.NetTools;
import gov.usgs.util.Util;
import gov.usgs.winston.Channel;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.WWSCommandString;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Example command string:
 * STATUS:
 *
 * @author Tom Parker
 */
public class StatusCommand extends BaseCommand
{
	
	private Channels channels;
	private static Connections connections = Connections.getInstance();
	
	public StatusCommand(NetTools nt, WinstonDatabase db, WWS wws)
	{
		super(nt, db, wws);
		channels = new Channels(db);
	}
	
	public void doCommand(Object info, SocketChannel channel)
	{
		
		WWSCommandString cmd = new WWSCommandString((String)info);
		double ageThreshold = Util.stringToDouble(cmd.getString(2), 0);
		double now = Util.ewToJ2K(System.currentTimeMillis()/1000);
		wws.log(Level.FINER, "STATUS: ", channel);

		StringBuilder sb = new StringBuilder();
		int lines = 0;
		Collection<ConnectionStatistics> css = connections.getConnectionStats();
		sb.append(String.format("Connection count: %d\n", css.size()));
		lines++;
		
		List<Channel> sts = channels.getChannels();
		sb.append(String.format("Channel count: %d\n", sts.size()));
		lines++;
		
		ArrayList<Double> ages = new ArrayList<Double>();
		for (Channel st : sts)
			if (st.getMaxTime() < now && (ageThreshold == 0 || now - st.getMaxTime() < ageThreshold))
				ages.add(now - st.getMaxTime());
		
		if (ages.size() == 0)
			ages.add(0d);
		
		Double[] d = new Double[ages.size()-1];
		d = ages.toArray(d);
		Arrays.sort(d);
		
		sb.append(String.format("Median data age: %s\n", d[(d.length-1)/2]));
		lines++;
		
		netTools.writeString("GC: " + lines + "\n" + sb.toString(), channel);

		wws.log(Level.FINER, "STATUS: ", channel);
	}
}