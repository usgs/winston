package gov.usgs.volcanoes.winston.in.ew;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.math.Goertzel;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;

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

    preambleFreq = StringUtils.stringToDouble(cf.getString("preambleFreq"), 21.25);
    threshold = StringUtils.stringToDouble(cf.getString("threshold"), 500);
    terminal = StringUtils.stringToBoolean(cf.getString("terminal"), false);
  }

  @Override
  public boolean match(final TraceBuf tb, final Options options) {

    final double g = Goertzel.goertzel(preambleFreq, tb.samplingRate(), tb.samples(), false);
    final double g2 = Goertzel.goertzel(preambleFreq / 3, tb.samplingRate(), tb.samples(), false);
    final double g3 = Goertzel.goertzel(preambleFreq / 6, tb.samplingRate(), tb.samples(), false);
    final double g4 = Goertzel.goertzel(preambleFreq / 9, tb.samplingRate(), tb.samples(), false);

    if (tb.channel().contains("EHZ") && (g / (g2 + g3 + g4)) > threshold) // TOMPTEMP
    {
      addMetadata("calPulse", String.format("%.2f", J2kSec.asEpoch(tb.firstSampleTime())));
      return true;
    } else
      return false;
  }

  @Override
  public String toString() {
    return String.format("McCalPulseFilter: accept");
  }
}
