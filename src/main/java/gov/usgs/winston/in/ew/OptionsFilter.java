package gov.usgs.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.ConfigFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2006/04/01 23:43:49  cervelli
 * Clean up.
 *
 * @author Dan Cervelli
 */
public class OptionsFilter
{
	private String name;
	private Options options;
	private List<SCNLFilter> filters;
	
	public OptionsFilter(String n, ConfigFile cf, Options def)
	{
		name = n;
		options = Options.createOptions(cf, def);
		
		String at = cf.getString("applyTo");
		if (at == null)
			return;
	
		String[] fs = at.split(",");
		filters = new ArrayList<SCNLFilter>(fs.length);
		for (String f : fs)
		{
			SCNLFilter filter = new SCNLFilter(f.trim());
			filters.add(filter);
		}
	}
	
	public String getName()
	{
		return name;
	}
	
	public boolean match(TraceBuf tb)
	{
		for (SCNLFilter filter : filters)
		{
			if (filter.match(tb, null))
				return true;
		}
		return false;
	}
	
	public Options getOptions()
	{
		return options;
	}
	
	public String toString()
	{
		return options.toString();
	}
	
}
