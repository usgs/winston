package gov.usgs.volcanoes.winston.in.ew;
import gov.usgs.earthworm.message.TraceBuf;

/**
 * 
 * @author Tom Parker
 */
public class JumboTracebufFilter extends TraceBufFilter 
{
	public JumboTracebufFilter()
	{
		super();
	}
	
	// todo: reimplement
	public boolean match(TraceBuf tb, Options options)
	{
		return (false);
	}

	public String toString()
	{
		return "JumboTracebufFilter";
	}
}
