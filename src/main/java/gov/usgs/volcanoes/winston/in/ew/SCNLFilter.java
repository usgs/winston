package gov.usgs.volcanoes.winston.in.ew;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;

/**
 *
 * $Log: not supported by cvs2svn $
 * 
 * @author Dan Cervelli
 */
public class SCNLFilter extends TraceBufFilter {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportEW.class);

  private String station;
  private String channel;
  private String network;
  private String location;

  public SCNLFilter() {
    station = "*";
    channel = "*";
    network = "*";
    location = "*";
  }

  public SCNLFilter(final String scnl) {
    setFilter(scnl);
  }

  public SCNLFilter(final String s, final String c, final String n, final String l) {
    keepRejects = false;
    // quiet = true;
    station = s;
    channel = c;
    network = n;
    location = l;
  }

  public void setFilter(final String scnl) {
    final String[] ss = scnl.split(" ");
    if (ss.length != 4) {
      LOGGER.warn("SCNLFilter: scnl must have four space-separated fields.");
      return;
    }

    station = ss[0];
    channel = ss[1];
    network = ss[2];
    location = ss[3];
  }

  @Override
  public void configure(final ConfigFile cf) {
    super.configure(cf);
    if (cf == null)
      return;

    final String scnl = cf.getString("scnl");
    if (scnl == null)
      return;

    setFilter(scnl);
  }

  private boolean test(final String crit, final String val) {
    if (crit == null || crit.equals("*") || crit.equals(val))
      return true;

    return Pattern.matches(crit, val);
  }

  @Override
  public boolean match(final TraceBuf tb, final Options options) {
    if (!test(station, tb.station()))
      return false;

    if (!test(channel, tb.channel()))
      return false;

    if (!test(network, tb.network()))
      return false;

    if (!test(location, tb.location()))
      return false;

    return true;
  }

  @Override
  public String toString() {
    return String.format("SCNLFilter [%s: %s %s %s %s]", accept ? "accept" : "reject", station,
        channel, network, location);
  }

}
