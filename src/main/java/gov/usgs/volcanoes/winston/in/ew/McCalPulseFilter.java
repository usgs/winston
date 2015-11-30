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

	public void configure(ConfigFile cf)
	{
		super.configure(cf);
		if (cf == null)
			return;
		
		preambleFreq = Util.stringToDouble(cf.getString("preambleFreq"), 21.25);
		threshold = Util.stringToDouble(cf.getString("threshold"), 500);
		terminal = Util.stringToBoolean(cf.getString("terminal"), false);
	}
	
	public boolean match(TraceBuf tb, Options options)
	{
		if (tb.firstSampleTime() == Double.NaN)
			return false;
		
		double g = Goertzel.goertzel(preambleFreq, tb.samplingRate(), tb.samples(), false);
		double g2 = Goertzel.goertzel(preambleFreq/3, tb.samplingRate(), tb.samples(), false);
		double g3 = Goertzel.goertzel(preambleFreq/6, tb.samplingRate(), tb.samples(), false);
		double g4 = Goertzel.goertzel(preambleFreq/9, tb.samplingRate(), tb.samples(), false);

		if (tb.channel().contains("EHZ") && (g/(g2+g3+g4)) > threshold) //TOMPTEMP
		{ 
			addMetadata("calPulse", String.format("%.2f", Util.ewToJ2K(tb.firstSampleTime())));
			return true;
		}
		else
			return false;
	}

	public String toString()
	{
		return String.format("McCalPulseFilter: accept");
	}
}
