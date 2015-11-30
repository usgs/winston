package gov.usgs.volcanoes.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.math.Goertzel;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Util;

/**
 * Filter to detect a McVCO calibration pulse
 *
 * Still not sure this is a good idea.
 *
 * @author Tom Parker
 */

public class McCalPulseFilter extends TraceBufFilter {
  private double preambleFreq;
  private double threshold;

  @Override
  public void configure(final ConfigFile cf) {
    super.configure(cf);
    if (cf == null)
      return;

    preambleFreq = Util.stringToDouble(cf.getString("preambleFreq"), 21.25);
    threshold = Util.stringToDouble(cf.getString("threshold"), 500);
    terminal = Util.stringToBoolean(cf.getString("terminal"), false);
  }

  @Override
  public boolean match(final TraceBuf tb, final Options options) {
    if (tb.firstSampleTime() == Double.NaN)
      return false;

    final double g = Goertzel.goertzel(preambleFreq, tb.samplingRate(), tb.samples(), false);
    final double g2 = Goertzel.goertzel(preambleFreq / 3, tb.samplingRate(), tb.samples(), false);
    final double g3 = Goertzel.goertzel(preambleFreq / 6, tb.samplingRate(), tb.samples(), false);
    final double g4 = Goertzel.goertzel(preambleFreq / 9, tb.samplingRate(), tb.samples(), false);

    if (tb.channel().contains("EHZ") && (g / (g2 + g3 + g4)) > threshold) // TOMPTEMP
    {
      addMetadata("calPulse", String.format("%.2f", Util.ewToJ2K(tb.firstSampleTime())));
      return true;
    } else
      return false;
  }

  @Override
  public String toString() {
    return String.format("McCalPulseFilter: accept");
  }
}
