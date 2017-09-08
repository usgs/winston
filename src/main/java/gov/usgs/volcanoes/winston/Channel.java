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

  private final int sid;
  private Instrument instrument;

  /** My SCNL */
  public final Scnl scnl;
  private final TimeSpan timeSpan;

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
    scnl = null;
    instrument = Instrument.NULL;
    timeSpan = new TimeSpan(Long.MAX_VALUE, Long.MIN_VALUE);
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
  public Channel(final int s, final Scnl c, final double min, final double max) {
    sid = s;
    scnl = c;
    timeSpan = new TimeSpan(J2kSec.asEpoch(min), J2kSec.asEpoch(max));
  }



  /**
   * Constructor from a String
   *
   * @param s
   *          colon-separated string of values defining a Channel
   * @throws UtilException When code cannot be parsed
   */
  public Channel(final String s) throws UtilException {
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
          addGroup(g);
      }
    }
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
    return DbUtils.scnlAsWinstonCode(scnl);
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
    return J2kSec.fromEpoch(timeSpan.startTime);
  }

  /**
   * Getter for max time
   *
   * @return max time
   */
  public double getMaxTime() {
    return J2kSec.fromEpoch(timeSpan.endTime);
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
   * @param linearA linearA
   */
  public void setLinearA(final double linearA) {
    if (linearA == 1e300) {
      this.linearA = Double.NaN;
    } else {
      this.linearA = linearA;
    }
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
   * @param linearB libearB
   */
  public void setLinearB(final double linearB) {
    if (linearB == 1e300) {
      this.linearB = Double.NaN;
    } else {
      this.linearB = linearB;
    }
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
   * @param alias alias
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
   * @param unit unit
   */
  public void setUnit(final String unit) {
    this.unit = unit;
  }

  /**
   * Sort channels alphabeticaly by n, s, c, l, with empty fields floating to
   * the top
   */
  public int compareTo(final Channel o) {
    return scnl.compareTo(o.scnl);
  }

  /**
   * Provide instrument.
   * 
   * @param instrument instrument
   */
  public void setInstrument(Instrument instrument) {
    this.instrument = instrument;
  }
}
