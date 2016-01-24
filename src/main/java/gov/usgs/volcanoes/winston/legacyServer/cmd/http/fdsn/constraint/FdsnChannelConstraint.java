package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.winston.Channel;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnChannelConstraint implements FdsnConstraint {

  public final String network;
  public final String station;
  public final String channel;
  public final String location;

  /**
   * channel constraints submitted through a POST will also have a time
   * constraint.
   */
  private FdsnTimeSimpleConstraint timeConstraint;

  public FdsnChannelConstraint(final String station, final String channel, final String network,
      final String location) {

    this.station = stringToString(station, ".*");
    this.channel = stringToString(channel, ".*");
    this.network = stringToString(network, ".*");
    this.location = stringToString(location, ".*");

    if (this.station == null)
      System.out.println("is null");
    else if (this.station.equals("null"))
      System.out.println("is text null");
  }

  public void setTimeConstraint(final FdsnTimeSimpleConstraint c) {
    timeConstraint = c;
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

  public FdsnTimeSimpleConstraint getTimeConstraint() {
    return timeConstraint;
  }

  @Override
  public String toString() {
    return "FdsnChannelConstraint: " + station + ":" + channel + ":" + network + ":" + location;
  }

}
