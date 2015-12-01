package gov.usgs.volcanoes.winston.in.ew;

import gov.usgs.util.Util;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.time.CurrentTime;

/**
 *
 * $Log: not supported by cvs2svn $
 * 
 * @author Dan Cervelli
 */
public class Options {
  public double timeThreshold;
  public int bufThreshold;
  public int maxBacklog;
  public boolean rsamEnable;
  public int rsamDelta;
  public int rsamDuration;
  public int maxDays;

  public Options() {}

  public static Options createOptions(final ConfigFile cf, final Options defaults) {
    final Options threshold = new Options();
    threshold.timeThreshold =
        Util.stringToDouble(cf.getString("timeThreshold"), defaults.timeThreshold);
    threshold.bufThreshold =
        Util.stringToInt(cf.getString("traceBufThreshold"), defaults.bufThreshold);
    threshold.maxBacklog = Util.stringToInt(cf.getString("maxBacklog"), defaults.maxBacklog);
    threshold.rsamEnable = Util.stringToBoolean(cf.getString("rsam.enable"), defaults.rsamEnable);
    threshold.rsamDelta = Util.stringToInt(cf.getString("rsam.delta"), defaults.rsamDelta);
    threshold.rsamDuration = Util.stringToInt(cf.getString("rsam.duration"), defaults.rsamDuration);
    threshold.maxDays = Util.stringToInt(cf.getString("maxDays"), defaults.maxDays);
    return threshold;
  }

  public boolean thresholdExceeded(final double time, final int size) {
    if (timeThreshold != -1) {
      final double dt = CurrentTime.getInstance().nowJ2k() - time;
      if (dt > timeThreshold)
        return true;
    }

    if (size > bufThreshold)
      return true;

    return false;
  }

  @Override
  public String toString() {
    return String.format(
        "timeThreshold=%.1f, traceBufThreshold=%d, maxBacklog=%d, maxDays=%d, rsam.enable=%s, rsam.delta=%d, rsam.duration=%d",
        timeThreshold, bufThreshold, maxBacklog, maxDays, rsamEnable, rsamDelta, rsamDuration);
  }
}
