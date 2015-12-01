package gov.usgs.volcanoes.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.time.CurrentTime;
import gov.usgs.volcanoes.core.util.StringUtils;

/**
 * A TraceBuf filter that rejects packets based on the difference between
 * their time and the current time.
 *
 * $Log: not supported by cvs2svn $
 * 
 * @author Dan Cervelli
 */
public class TimeFilter extends TraceBufFilter {
  private double tLessThan;
  private double tGreaterThan;

  public TimeFilter() {}

  public TimeFilter(final double tlt, final double tgt) {
    this();
    tLessThan = tlt;
    tGreaterThan = tgt;
  }

  @Override
  public void configure(final ConfigFile cf) {
    super.configure(cf);
    if (cf == null)
      return;

    tLessThan = StringUtils.stringToDouble(cf.getString("past"), Double.NaN);
    tGreaterThan = StringUtils.stringToDouble(cf.getString("future"), Double.NaN);
  }

  @Override
  public boolean match(final TraceBuf tb, final Options options) {
    final double dt = tb.getStartTimeJ2K() - CurrentTime.getInstance().nowJ2k();
    if (!Double.isNaN(tLessThan) && dt < tLessThan)
      return true;

    if (!Double.isNaN(tGreaterThan) && dt > tGreaterThan)
      return true;

    return false;
  }

  @Override
  public String toString() {
    return String.format("TimeFilter [%s: %.2f > dt > %.2f]", accept ? "accept" : "reject",
        tLessThan, tGreaterThan);
  }
}
