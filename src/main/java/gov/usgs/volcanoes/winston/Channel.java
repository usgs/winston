package gov.usgs.volcanoes.winston;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gov.usgs.volcanoes.core.time.J2kSec;


/**
 * A class representing one row of the channels table.
 *
 * @author Dan Cervelli
 */
public class Channel implements Comparable<Channel> {
  public static final int ONE_DAY = 24 * 60 * 60;

  private final int sid;
  private Instrument instrument;

  private final String code;
  private final double minTime;
  private final double maxTime;

  public final String station;
  public final String channel;
  public final String network;
  public final String location;

  private double linearA;
  private double linearB;
  private String alias;
  private String unit;

  private List<String> groups;

  private Map<String, String> metadata;

  /**
   * Default constructor
   */
  public Channel() {
    sid = -1;
    code = null;
    instrument = Instrument.NULL;
    minTime = Double.NaN;
    maxTime = Double.NaN;
    station = "";
    channel = "";
    network = "";
    location = "--";
  }

  /**
   * Constructor from minimal info
   *
   * @param s
   *          sid
   * @param c
   *          code
   * @param min
   *          minTime
   * @param max
   *          maxTime
   */
  public Channel(final int s, final String c, final double min, final double max) {
    sid = s;
    code = c;
    minTime = min;
    maxTime = max;

    final String[] cmp = code.split("$");

    if (cmp.length > 0)
      station = cmp[0];
    else
      station = "";

    if (cmp.length > 1)
      channel = cmp[1];
    else
      channel = "";

    if (cmp.length > 2)
      network = cmp[2];
    else
      network = "";

    if (cmp.length > 3)
      location = cmp[0];
    else
      location = "--";

  }

  /**
   * Constructor from a ResultSet
   *
   * @param rs
   *          ResultSet
   * @throws SQLException
   */
  public Channel(final ResultSet rs) throws SQLException {
    this(rs, Integer.MAX_VALUE);
  }
  
  public Channel(final ResultSet rs, int aparentRetention) throws SQLException {
    sid = rs.getInt("sid");
    code = rs.getString("code");
    
    
    double mt = rs.getDouble("st");
    minTime = Double.max(mt, J2kSec.now() - aparentRetention);
    maxTime = rs.getDouble("et");

    instrument = new Instrument(rs);
    linearA = rs.getDouble("linearA");
    if (linearA == 1e300)
      linearA = Double.NaN;
    linearB = rs.getDouble("linearB");
    if (linearB == 1e300)
      linearB = Double.NaN;
    unit = rs.getString("unit");
    if (unit == null)
      unit = "";
    alias = rs.getString("alias");
    if (alias == null)
      alias = "";

    final String[] cmp = code.split("\\$");

    if (cmp.length > 0)
      station = cmp[0];
    else
      station = "";

    if (cmp.length > 1)
      channel = cmp[1];
    else
      channel = "";

    if (cmp.length > 2)
      network = cmp[2];
    else
      network = "";

    if (cmp.length > 3)
      location = cmp[0];
    else
      location = "--";
  }

  /**
   * Constructor from a String
   *
   * @param s
   *          colon-separated string of values defining a Channel
   */
  public Channel(final String s) {
    final String[] ss = s.split(":");
    sid = Integer.parseInt(ss[0]);
    code = ss[1];
    minTime = Double.parseDouble(ss[2]);
    maxTime = Double.parseDouble(ss[3]);
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
          addGroup(g);
      }
    }

    final String[] cmp = code.split("\\$");

    if (cmp.length > 0)
      station = cmp[0];
    else
      station = "";

    if (cmp.length > 1)
      channel = cmp[1];
    else
      channel = "";

    if (cmp.length > 2)
      network = cmp[2];
    else
      network = "";

    if (cmp.length > 3)
      location = cmp[0];
    else
      location = "--";

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
   * Add g to list of groups
   *
   * @param g
   *          group to add
   */
  public void addGroup(final String g) {
    if (groups == null)
      groups = new ArrayList<String>(2);
    groups.add(g);
  }

  /**
   * Getter for sid
   *
   * @return sid
   */
  public int getSID() {
    return sid;
  }

  /**
   * Getter for code
   *
   * @return code
   */
  public String getCode() {
    return code;
  }

  /**
   * Getter for instrument
   *
   * @return instrument
   */
  public Instrument getInstrument() {
    return instrument;
  }

  /**
   * Getter for min time
   *
   * @return min time
   */
  public double getMinTime() {
    return minTime;
  }

  /**
   * Getter for max time
   *
   * @return max time
   */
  public double getMaxTime() {
    return maxTime;
  }

  /**
   * Getter for code
   *
   * @return code
   */
  @Override
  public String toString() {
    return code;
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
   *
   * @return metadata as a string
   */
  public String toMetadataString(final int maxDays) {
    return String.format("%s:%s:%s:%s:%f:%f:%s:", toPV2String(maxDays), instrument.getTimeZone(),
        alias, unit, linearA, linearB, getGroupString());
  }

  public String toMetadataString() {
    return toMetadataString(0);
  }

  /**
   * Getter for PV2 as a :-separated string
   *
   * @return PV2 as a string
   */
  public String toPV2String(final int maxDays) {
    double min = minTime;
    double max = maxTime;

    if (maxDays > 0) {
      min = Math.max(min, J2kSec.now() - (maxDays * ONE_DAY));
      max = Math.max(max, J2kSec.now() - (maxDays * ONE_DAY));
    }

    return String.format("%d:%s:%f:%f:%f:%f", sid, code, minTime, maxTime,
        instrument.getLongitude(), instrument.getLatitude());
  }

  public String toPV2String() {
    return toPV2String(0);
  }

  /**
   * Getter for VDX as a :-separated string
   *
   * @return VDX as a string
   */
  public String toVDXString() {
    // this contains the new output for what VDX is expecting
    final String stripped = code.replace('$', ' ');
    // return String.format("%s:%f:%f:%s:%s", code,
    // instrument.getLongitude(), instrument.getLatitude(), stripped,
    // stripped);
    return String.format("%d:%s:%s:%f:%f:%f:%s", sid, code, stripped, instrument.getLongitude(),
        instrument.getLatitude(), instrument.getHeight(), "0");
  }

  /**
   * Getter for linearA
   *
   * @return linearA
   */
  public double getLinearA() {
    return linearA;
  }

  /**
   * Setter for linearA
   *
   * @param linearA
   */
  public void setLinearA(final double linearA) {
    this.linearA = linearA;
  }

  /**
   * Getter for linearB
   *
   * @return linearB
   */
  public double getLinearB() {
    return linearB;
  }

  /**
   * Setter for linearB
   *
   * @param linearB
   */
  public void setLinearB(final double linearB) {
    this.linearB = linearB;
  }

  /**
   * Getter for alias
   *
   * @return alias
   */
  public String getAlias() {
    if (alias == null || alias.length() == 0)
      return null;

    return alias;
  }

  /**
   * Setter for alias
   *
   * @param alias
   */
  public void setAlias(final String alias) {
    this.alias = alias;
  }

  /**
   * Getter for unit
   *
   * @return unit
   */
  public String getUnit() {
    if (unit == null || unit.length() == 0)
      return null;

    return unit;
  }

  /**
   * Setter for unit
   *
   * @param unit
   */
  public void setUnit(final String unit) {
    this.unit = unit;
  }

  /**
   * Sort channels alphabeticaly by n, s, c, l, with empty fields floating to
   * the top
   */
  public int compareTo(final Channel o) {
    if (!network.equals(o.network))
      return network.compareTo(o.network);

    if (!station.equals(o.station))
      return station.compareTo(o.station);

    if (!channel.equals(o.channel))
      return channel.compareTo(o.channel);

    return location.compareTo(o.location);
  }
}
