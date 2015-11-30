package gov.usgs.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;

/**
 * A TraceBuf filter that rejects packets based on the difference between
 * their time and the current time.
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class TimeFilter extends TraceBufFilter
{
	private double tLessThan;
	private double tGreaterThan;
	
	public TimeFilter()
	{}
	
	public TimeFilter(double tlt, double tgt)
	{
		this();
		tLessThan = tlt;
		tGreaterThan = tgt;
	}
	
	public void configure(ConfigFile cf)
	{
		super.configure(cf);
		if (cf == null)
			return;
		
		tLessThan = Util.stringToDouble(cf.getString("past"), Double.NaN);
		tGreaterThan = Util.stringToDouble(cf.getString("future"), Double.NaN);
	}
	
	public boolean match(TraceBuf tb, Options options)
	{
		double dt = tb.getStartTimeJ2K() - CurrentTime.getInstance().nowJ2K();
		if (!Double.isNaN(tLessThan) && dt < tLessThan)
			return true;
		
		if (!Double.isNaN(tGreaterThan) &&  dt > tGreaterThan)
			return true;
		
		return false;
	}

	public String toString()
	{
		return String.format("TimeFilter [%s: %.2f > dt > %.2f]", 
				accept ? "accept" : "reject", tLessThan, tGreaterThan);
	}
}
