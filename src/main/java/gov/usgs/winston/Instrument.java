package gov.usgs.winston;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.3  2006/08/03 22:10:02  cervelli
 * Checks if lon/lat or was null.
 *
 * Revision 1.2  2006/08/03 19:01:12  cervelli
 * Time zone methods.
 *
 * Revision 1.1  2006/08/02 23:30:58  cervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class Instrument
{
	public static Instrument NULL = new Instrument();
	
	private int iid;
	private String name;
	private String description;
	private double longitude = -999;
	private double latitude = -999;
	private double height = -999;
	
	private String timeZone;
	
	private Map<String, String> metadata;
	
	public Instrument() {}
	
	public Instrument(ResultSet rs) throws SQLException 
	{
		iid = rs.getInt("instruments.iid");
		name = rs.getString("name");
		description = rs.getString("description");
		longitude = rs.getDouble("lon");
		boolean nll = rs.wasNull();
		latitude = rs.getDouble("lat");
		nll = nll || rs.wasNull();
		if (nll)
		{
			longitude = -999;
			latitude = -999;
		}
		height = rs.getDouble("height");
		timeZone = rs.getString("timezone");
	}
	
	public int getID()
	{
		return iid;
	}
	
	public void setMetadata(Map<String, String> map)
	{
		metadata = map;
	}
	
	public Map<String, String> getMetadata()
	{
		return metadata;
	}
	
	public void setLongitude(double d)
	{
		longitude = d;
	}
	
	public void setLatitude(double d)
	{
		latitude = d;
	}
	
	public double getLongitude()
	{
		if (longitude == -999)
			return Double.NaN;
		return longitude;
	}
	
	public double getLatitude()
	{
		if (latitude == -999)
			return Double.NaN;
		return latitude;
	}
	
	public double getHeight()
	{
		if (height == -999)
			return Double.NaN;
		return height;
	}
	
	public void setTimeZone(String s)
	{
		timeZone = s;
	}
	
	public String getTimeZone()
	{
		return timeZone == null ? "" : timeZone;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public String toString()
	{
		return String.format("%d:%s:%s:%f:%f:%f", iid, name, description, longitude, latitude, height);
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(int iid) {
		this.iid = iid;
	}
}
