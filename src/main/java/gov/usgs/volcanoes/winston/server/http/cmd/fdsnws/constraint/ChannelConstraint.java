/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnException;

/**
 * Channel constraint.
 * 
 * @author Tom Parker
 *
 */
@SuppressWarnings("deprecation")
public class ChannelConstraint extends FdsnConstraint {

  private final static Logger LOGGER = LoggerFactory.getLogger(ChannelConstraint.class);

  private final String network;
  private final String station;
  private final String channel;
  private final String location;
  private TimeSimpleConstraint timeConstraint;

  /**
   * Constructor.
   * 
   * @param station station
   * @param channel channel
   * @param network network
   * @param location location
   */
  public ChannelConstraint(final String station, final String channel, final String network,
      final String location) {

    this.station = stringToString(station, "*");
    this.channel = stringToString(channel, "*");
    this.network = stringToString(network, "*");
    this.location = stringToString(location, "*");

    LOGGER.info("channel constraint: {}:{}:{}:{}", this.station, this.channel, this.network,
        this.location);
  }

  public static ChannelConstraint build(Map<String, String> arguments) throws FdsnException {
    String station = regExify(getArg(arguments, "station", "sta"));
    String channel = regExify(getArg(arguments, "channel", "cha"));
    String network = regExify(getArg(arguments, "network", "net"));
    String location = regExify(getArg(arguments, "location", "loc"));
    
    String startTime = getArg(arguments, "starttime", "start");
    String endTime = getArg(arguments, "endtime", "end");

    ChannelConstraint chanConstraint = new ChannelConstraint(station, channel, network, location);
    chanConstraint.setTimeConstraint(new TimeSimpleConstraint(startTime, endTime));
    
    return chanConstraint;
  }

  public static List<ChannelConstraint> buildMulti(Map<String, String> arguments) throws FdsnException {
    List<ChannelConstraint> constraints = new ArrayList<ChannelConstraint>();
    
    final String chans = arguments.get("chans");
    if (chans != null) {
      String[] chan = chans.split("\n");
      for (String ch : chan) {
        String[] chanParts = ch.split("\\s");
        ChannelConstraint channelConstraint = new ChannelConstraint(chanParts[1], chanParts[3], chanParts[0], chanParts[2]);
        TimeSimpleConstraint timeConstraint = new TimeSimpleConstraint(chanParts[4], chanParts[5]);
        channelConstraint.setTimeConstraint(timeConstraint);
        constraints.add(channelConstraint);
      }
    }
    return constraints;
  }

  /**
   * timeConstraint mutator.
   * 
   * @param timeConstraint timeConstraint.
   */
  public void setTimeConstraint(final TimeSimpleConstraint timeConstraint) {
    LOGGER.info("adding time constraint {}", timeConstraint);
    this.timeConstraint = timeConstraint;
  }

  private String stringToString(final String in, final String def) {
    if (in == null)
      return def;
    if ("".equals(in))
      return def;
    if ("null".equals(in))
      return def;

    return in;
  }

  /**
   * match Channel.
   * 
   * @param chan Channel
   * @return true if matches
   */
  public boolean matches(final Channel chan) {

    if (timeConstraint.matches(chan))
      return nameMatches(chan);
    else
      return false;
  }

  public boolean nameMatches(final Channel chan) {
    if (chan == null)
      return false;

    final String net = chan.scnl.network;
    if (net != null && !net.matches(network))
      return false;

    final String cha = chan.scnl.channel;
    if (cha != null && !cha.matches(channel))
      return false;

    final String sta = chan.scnl.station;
    if (sta != null && !sta.matches(station))
      return false;

    final String loc = chan.scnl.location;
    if (loc != null && !loc.matches(location))
      return false;
    
    return true;
  }
  
  /**
   * match TraceBuf.
   * 
   * @param buf tracebuf
   * @return true if matches
   */
  public boolean matches(final TraceBuf buf) {

    if (buf == null)
      return false;

    final String net = buf.network();
    if (net != null && !net.matches(network))
      return false;

    final String cha = buf.channel();
    if (cha != null && !cha.matches(channel))
      return false;

    final String sta = buf.station();
    if (sta != null && !sta.matches(station))
      return false;

    final String loc = buf.location();
    if (loc != null && !loc.matches(location))
      return false;

    if (timeConstraint != null)
      return timeConstraint.matches(buf);

    return true;
  }

  /**
   * timeConstraint accessor.
   *
   * @return timeConstraint
   */
  public TimeSimpleConstraint getTimeSimpleConstraint() {
    return timeConstraint;
  }

  @Override
  public String toString() {
    return "FdsnChannelConstraint: " + station + ":" + channel + ":" + network + ":" + location;
  }

  private static String regExify(String inString) {
    String outString = inString.replace("*", ".*");
    outString = outString.replace("?", ".{1}*");

    return outString;
  }
 }
