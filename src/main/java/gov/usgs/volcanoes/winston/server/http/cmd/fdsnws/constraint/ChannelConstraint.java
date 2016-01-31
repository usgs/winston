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

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnException;

/**
 * Channel constraint.
 * 
 * @author Tom Parker
 *
 */
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

    this.station = stringToString(station, ".*");
    this.channel = stringToString(channel, ".*");
    this.network = stringToString(network, ".*");
    this.location = stringToString(location, ".*");

    LOGGER.info("channel constraint: {}:{}:{}:{}", this.station, this.channel, this.network,
        this.location);
  }

  public static ChannelConstraint build(Map<String, String> arguments) {
    String station = getArg(arguments, "station", "sta");
    String channel = getArg(arguments, "channel", "cha");
    String network = getArg(arguments, "network", "net");
    String location = getArg(arguments, "location", "loc");
    
    return new ChannelConstraint(station, channel, network, location);
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

    if (chan == null)
      return false;

    final String net = chan.network;
    if (net != null && !net.matches(network))
      return false;

    final String cha = chan.channel;
    if (cha != null && !cha.matches(channel))
      return false;

    final String sta = chan.station;
    if (sta != null && !sta.matches(station))
      return false;

    final String loc = chan.location;
    if (loc != null && !loc.matches(location))
      return false;

    if (timeConstraint != null)
      return timeConstraint.matches(chan);

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
  public TimeConstraint getTimeConstraint() {
    return timeConstraint;
  }

  @Override
  public String toString() {
    return "FdsnChannelConstraint: " + station + ":" + channel + ":" + network + ":" + location;
  }

 }
