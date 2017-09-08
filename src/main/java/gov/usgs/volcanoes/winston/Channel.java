package gov.usgs.volcanoes.winston;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.DbUtils;


/**
 * A class representing one row of the channels table.
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class Channel implements Comparable<Channel> {

  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Channel.class);

  /** Number of seconds in a typical day */
  public static final int ONE_DAY = 24 * 60 * 60;

  public final int sid;
  public final Instrument instrument;

  /** My SCNL */
  public final Scnl scnl;
  public final TimeSpan timeSpan;

  public final double linearA;
  public final double linearB;
  public final String alias;
  public final String unit;

  private List<String> groups;
  private Map<String, String> metadata;


  public static class Builder {

    private int sid = -1;
    private Instrument instrument = Instrument.NULL;
    private Scnl scnl;
    private TimeSpan timeSpan = new TimeSpan(Long.MAX_VALUE, Long.MIN_VALUE);

    private double linearA = Double.NaN;
    private double linearB = Double.NaN;
    private String alias;
    private String unit;

    private List<String> groups;
    private Map<String, String> metadata;

    public Builder sid(int sid) {
      this.sid = sid;
      return this;
    }

    public Builder instrument(Instrument instrument) {
      this.instrument = instrument;
      return this;
    }

    public Builder scnl(Scnl scnl) {
      this.scnl = scnl;
      return this;
    }

    public Builder timeSpan(TimeSpan timeSpan) {
      this.timeSpan = timeSpan;
      return this;
    }

    public Builder linearA(double linearA) {
      this.linearA = linearA;
      return this;
    }
   
    public Builder linearB(double linearB) {
      this.linearB = linearB;
      return this;
    }
    
    public Builder alias(String alias) {
      this.alias = alias == null ? "" : alias;
      return this;
    }
    
    public Builder unit(String unit) {
      this.unit = unit == null ? "" : unit;
      return this;
    }

    public Builder group(String group) {
      if (groups == null)
        groups = new ArrayList<String>(2);
      groups.add(group);
      return this;
    }

    public Channel build() {
      return new Channel(this);
    }
    
    public Builder parse(String s) throws UtilException {
      final String[] ss = s.split(":");
      sid = Integer.parseInt(ss[0]);
      scnl = Scnl.parse(ss[1]);
      double minTime = Double.parseDouble(ss[2]);
      double maxTime = Double.parseDouble(ss[3]);
      timeSpan = new TimeSpan(J2kSec.asEpoch(minTime), J2kSec.asEpoch(maxTime));
      instrument = new Instrument();
      instrument.setLongitude(Double.parseDouble(ss[4]));
      instrument.setLatitude(Double.parseDouble(ss[5]));
      if (ss.length == 12) // metadata present
      {
        if (ss[6].length() >= 1)
          instrument.setTimeZone(ss[6]);
        if (ss[7].length() >= 1)
          alias = ss[7];
        if (ss[8].length() >= 1)
          unit = ss[8];
        linearA = Double.parseDouble(ss[9]);
        linearB = Double.parseDouble(ss[10]);
        if (!ss[11].equals("~")) {
          final String[] gs = ss[11].split("\\|");
          for (final String g : gs)
            group(g);
        }
      } 
      
      return this;
    }
  }



  /**
   * Default constructor
   */
  public Channel(Builder builder) {
    sid = builder.sid;
    scnl = builder.scnl;
    timeSpan = builder.timeSpan;
    instrument = builder.instrument;
    linearA = builder.linearA;
    linearB = builder.linearB;
    alias = builder.alias;
    unit = builder.unit;
  }

 
  /**
   * Setter for metadata
   *
   * @param map
   *          Mapping of metadata keys to values
   */
  public void setMetadata(final Map<String, String> map) {
    metadata = map;
  }

  /**
   * Getter for metadata
   *
   * @return mapping of metadata keys to values
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }

  /**
   * Getter for groups as a |-separated string
   *
   * @return groups as a string
   */
  public String getGroupString() {
    if (groups == null)
      return "~";

    String gs = "";
    for (int i = 0; i < groups.size() - 1; i++) {
      gs += groups.get(i) + "|";
    }
    gs += groups.get(groups.size() - 1);
    return gs;
  }

  /**
   * Getter for List of groups
   *
   * @return List of groups
   */
  public List<String> getGroups() {
    return groups;
  }

  /**
   * Getter for metadata as a :-separated string
   * @return metadata as a string
   */
  public String toMetadataString() {
    return String.format("%s:%s:%s:%s:%f:%f:%s:", toPV2String(), instrument.getTimeZone(),
        alias, unit, linearA, linearB, getGroupString());
  }

  /**
   * Getter for PV2 as a :-separated string
   *
   * @return PV2 as a string
   */
  public String toPV2String() {
    double min = J2kSec.fromEpoch(timeSpan.startTime);
    double max = J2kSec.fromEpoch(timeSpan.endTime);

    return String.format("%d:%s:%f:%f:%f:%f", sid, DbUtils.scnlAsWinstonCode(scnl), min, max,
        instrument.getLongitude(), instrument.getLatitude());
  }

  /**
   * Getter for VDX as a :-separated string
   *
   * @return VDX as a string
   */
  public String toVDXString() {
    // this contains the new output for what VDX is expecting
    final String stripped = scnl.toString(" ");
    // return String.format("%s:%f:%f:%s:%s", code,
    // instrument.getLongitude(), instrument.getLatitude(), stripped,
    // stripped);
    return String.format("%d:%s:%s:%f:%f:%f:%s", sid, DbUtils.scnlAsWinstonCode(scnl), stripped,
        instrument.getLongitude(),
        instrument.getLatitude(), instrument.getHeight(), "0");
  }

  /**
   * Return channel code. 
   */
  public String toString() {
    return DbUtils.scnlAsWinstonCode(scnl);
  }

  /**
   * Sort channels alphabeticaly by n, s, c, l, with empty fields floating to
   * the top
   */
  public int compareTo(final Channel o) {
    return scnl.compareTo(o.scnl);
  }
}
