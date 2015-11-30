package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint;

import java.text.ParseException;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.FdsnException;

/**
 * @author Tom Parker
 *
 */
public class FdsnTimeSimpleConstraint extends FdsnTimeConstraint {


  public final double startTime; // j2ksec
  public final double endTime; // j2ksec

  public FdsnTimeSimpleConstraint(final String startTime, final String endTime)
      throws FdsnException {
    try {
      this.startTime = dateStringToDouble(startTime, FAR_IN_PAST);
      this.endTime = dateStringToDouble(endTime, FAR_IN_FUTURE);
    } catch (final ParseException e) {
      throw new FdsnException(400, "Can't parse time constraint: " + this);
    }
  }

  public boolean matches(final Channel chan) {
    final double end = chan.getMaxTime();
    if (end != Double.NaN && chan.getMaxTime() > endTime)
      return false;

    final double start = chan.getMinTime();
    if (start != Double.NaN && chan.getMinTime() < startTime)
      return false;

    return true;
  }

  public boolean matches(final TraceBuf buf) {
    final double end = buf.lastSampleTime() + buf.samplingPeriod();
    if (end > endTime)
      return false;

    final double start = buf.firstSampleTime();
    if (start < startTime)
      return false;

    return true;
  }

  @Override
  public String toString() {
    return "FdsnTimeSimpleConstraint: " + Util.j2KToDateString(startTime) + ":"
        + Util.j2KToDateString(endTime);
  }


}
