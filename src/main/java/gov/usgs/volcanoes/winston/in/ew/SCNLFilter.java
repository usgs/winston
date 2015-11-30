package gov.usgs.volcanoes.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;

import java.util.regex.Pattern;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class SCNLFilter extends TraceBufFilter
{
	private String station;
	private String channel;
	private String network;
	private String location;
	
	public SCNLFilter()
	{
		station = "*";
		channel = "*";
		network = "*";
		location = "*";
	}
	
	public SCNLFilter(String scnl)
	{
		setFilter(scnl);
	}
	
	public SCNLFilter(String s, String c, String n, String l)
	{
		keepRejects = false;
//		quiet = true;
		station = s;
		channel = c;
		network = n;
		location = l;
	}
	
	public void setFilter(String scnl)
	{
		String[] ss = scnl.split(" ");
		if (ss.length != 4)
		{
			Log.getLogger("gov.usgs.winston.in.ew").warning(
					"SCNLFilter: scnl must have four space-separated fields.");
			return;
		}
		
		station = ss[0];
		channel = ss[1];
		network = ss[2];
		location = ss[3];
	}
	
	public void configure(ConfigFile cf)
	{
		super.configure(cf);
		if (cf == null)
			return;
		
		String scnl = cf.getString("scnl");
		if (scnl == null)
			return;
		
		setFilter(scnl);
	}
	
	private boolean test(String crit, String val)
	{
		if (crit == null || crit.equals("*") || crit.equals(val))
			return true;
			
		return Pattern.matches(crit, val);
	}
	
	public boolean match(TraceBuf tb, Options options)
	{
		if (!test(station, tb.station()))
			return false;
		
		if (!test(channel, tb.channel()))
			return false;
		
		if (!test(network, tb.network()))
			return false;
		
		if (!test(location, tb.location()))
			return false;
		
		return true;
	}
	
	public String toString()
	{
		return String.format("SCNLFilter [%s: %s %s %s %s]", 
				accept ? "accept" : "reject", station, channel, network, location);
	}

}
