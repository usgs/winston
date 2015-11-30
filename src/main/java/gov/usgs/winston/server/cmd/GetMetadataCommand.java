package gov.usgs.winston.server.cmd;

import gov.usgs.net.NetTools;
import gov.usgs.winston.Channel;
import gov.usgs.winston.Instrument;
import gov.usgs.winston.db.Channels;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.WWSCommandString;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Dan Cervelli
 */
public class GetMetadataCommand extends BaseCommand
{
	private Channels channels;
	
	public GetMetadataCommand(NetTools nt, WinstonDatabase db, WWS wws)
	{
		super(nt, db, wws);
		channels = new Channels(db);
	}
	
	private String escape(String s)
	{
		if (s == null)
			return "";
		return s.replaceAll(",", "\\\\c").replaceAll("\n", "\\\\n");
	}
	
	private void appendList(StringBuilder sb, String name, List<String> list)
	{
		sb.append(name);
		sb.append("=");
		if (list == null)
			return;
		for (String value : list)
		{
			sb.append(escape(value));
			sb.append("\\c");
		}
	}
	
	private void appendMap(StringBuilder sb, Map<String, String> map)
	{
		if (map == null)
			return;
		
		for (String key : map.keySet())
		{
			String value = map.get(key);
			sb.append(escape(key));
			sb.append("=");
			sb.append(escape(value));
			sb.append(",");
		}
	}
	
	private void getInstrumentMetadata(WWSCommandString cmd, SocketChannel channel)
	{
		List<Instrument> insts = channels.getInstruments();
		StringBuilder sb = new StringBuilder(insts.size() * 60);
		sb.append(String.format("%s %d\n", cmd.getID(), insts.size()));
		for (Instrument inst : insts)
		{
			sb.append("name=");
			sb.append(escape(inst.getName()));
			sb.append(",");
			sb.append("description=");
			sb.append(escape(inst.getDescription()));
			sb.append(",");
			sb.append("longitude=");
			sb.append(inst.getLongitude());
			sb.append(",");
			sb.append("latitude=");
			sb.append(inst.getLatitude());
			sb.append(",");
			sb.append("height=");
			sb.append(inst.getHeight());
			sb.append(",");
			sb.append("timezone=");
			sb.append(inst.getTimeZone());
			sb.append(",");
			appendMap(sb, inst.getMetadata());
			sb.append("\n");
		}
		netTools.writeString(sb.toString(), channel);
	}
	
	private void getChannelMetadata(WWSCommandString cmd, SocketChannel channel)
	{
		List<Channel> chs = channels.getChannels(true);
		StringBuilder sb = new StringBuilder(chs.size() * 60);
		sb.append(String.format("%s %d\n", cmd.getID(), chs.size()));
		for (Channel ch : chs)
		{
			sb.append("channel=");
			sb.append(ch.getCode().replace('$', ' '));
			sb.append(",");
			sb.append("instrument=");
			sb.append(escape(ch.getInstrument().getName()));
			sb.append(",");
			sb.append("startTime=");
			sb.append(timeOrMaxDays(ch.getMinTime()));
			sb.append(",");
			sb.append("endTime=");
			sb.append(timeOrMaxDays(ch.getMaxTime()));
			sb.append(",");
			sb.append("alias=");
			sb.append(escape(ch.getAlias()));
			sb.append(",");
			sb.append("unit=");
			sb.append(escape(ch.getUnit()));
			sb.append(",");
			sb.append("linearA=");
			sb.append(ch.getLinearA());
			sb.append(",");
			sb.append("linearB=");
			sb.append(ch.getLinearB());
			sb.append(",");
			appendList(sb, "groups", ch.getGroups());
			sb.append(",");
			appendMap(sb, ch.getMetadata());
			sb.append("\n");
		}
		netTools.writeString(sb.toString(), channel);
	}
	
	public void doCommand(Object info, SocketChannel channel)
	{
		WWSCommandString cmd = new WWSCommandString((String)info);
		String[] ss = cmd.getCommandSplits();
		if (ss.length <= 2)
			return;
		
		if (ss[2].equals("INSTRUMENT"))
			getInstrumentMetadata(cmd, channel);
		else if (ss[2].equals("CHANNEL"))
			getChannelMetadata(cmd, channel);
		
		wws.log(Level.FINER, "GETMETADATA.", channel);
	}
}
