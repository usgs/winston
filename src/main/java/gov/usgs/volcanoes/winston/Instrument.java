package gov.usgs.volcanoes.winston;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import gov.usgs.volcanoes.winston.Channel.Builder;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.3 2006/08/03 22:10:02 cervelli
 * Checks if lon/lat or was null.
 *
 * Revision 1.2 2006/08/03 19:01:12 cervelli
 * Time zone methods.
 *
 * Revision 1.1 2006/08/02 23:30:58 cervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class Instrument {
//  public final static Instrument NULL = new Builder().build();

  public final int iid;
  public final String name;
  public final String description;
  public final double longitude;
  public final double latitude;
  public final double height;
  public final String timeZone;
  public final Map<String, String> metadata;


  public static class Builder {
    private int iid;
    private String name;
    private String description;
    private double longitude = -999;
    private double latitude = -999;
    private double height = -999;
    private String timeZone;
    private Map<String, String> metadata;

    public Builder() {
      metadata = new HashMap<String, String>();
    }
    
    public Builder iid(int iid) {
      this.iid = iid;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder longitude(double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder latitude(double latitude) {
      this.longitude = latitude;
      return this;
    }

    public Builder height(double height) {
      this.longitude = height;
      return this;
    }

    public Builder timeZone(String timeZone) {
      this.timeZone = timeZone;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }
    
    public Builder parse(ResultSet rs) throws SQLException {
      iid = rs.getInt("instruments.iid");
      name = rs.getString("name");
      description = rs.getString("description");
      longitude = rs.getDouble("lon");
      boolean nll = rs.wasNull();
      latitude = rs.getDouble("lat");
      nll = nll || rs.wasNull();
      if (nll) {
        longitude = -999;
        latitude = -999;
      }
      height = rs.getDouble("height");
      timeZone = rs.getString("timezone");
      
      return this;
    }
    
    public Instrument build() {
      if (metadata == null) {
        metadata = new HashMap<String, String>();
      }
      
      return new Instrument(this);
    }
  
  }

  private Instrument(Builder builder) {
    iid = builder.iid;
    name = builder.name;
    description = builder.description;
    longitude = builder.longitude;
    latitude = builder.latitude;
    height = builder.height;
    timeZone = builder.timeZone;
    metadata = Collections.unmodifiableMap(builder.metadata);
  }

  @Override
  public String toString() {
    return String.format("%d:%s:%s:%f:%f:%f", iid, name, description, longitude, latitude, height);
  }
}
