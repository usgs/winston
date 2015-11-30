package gov.usgs.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.CurrentTime;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class MaxDaysFilter extends TraceBufFilter
{
	public MaxDaysFilter()
	{}
	
	public boolean match(TraceBuf tb, Options options)
	{
		if (options.maxDays <= 0)
			return false;
		
		double t = tb.getStartTimeJ2K();
		double ds = CurrentTime.getInstance().nowJ2K() - t;
		return (ds > ((double)options.maxDays * 86400.0));
	}

	public String toString()
	{
		return "MaxDaysFilter";
	}
}
