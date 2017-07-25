package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint;

import java.text.ParseException;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnException;

/**
 * @author Tom Parker
 *
 */
public class FdsnTimeSimpleConstraint extends FdsnTimeConstraint {


  public final double endTime; // j2ksec
  public final double startTime; // j2ksec

  public FdsnTimeSimpleConstraint(final String startTime, final String endTime)
      throws FdsnException {
    super();
    try {
      this.startTime = dateStringToDouble(startTime, FAR_IN_PAST);
      this.endTime = dateStringToDouble(endTime, FAR_IN_FUTURE);
    } catch (final ParseException e) {
      throw new FdsnException(400, "Can't parse time constraint: " + this);
    }
  }

  public boolean matches(final Channel chan) {
    final double end = chan.getMaxTime();
    if (!Double.isNaN(end) && chan.getMaxTime() > endTime) {
      return false;
    }

    final double start = chan.getMinTime();
    if (!Double.isNaN(start) && chan.getMinTime() < startTime) {
      return false;
    }

    return true;
  }

  public boolean matches(final TraceBuf buf) {
    final double end = buf.lastSampleTime() + buf.samplingPeriod();
    if (end > endTime) {
      return false;
    }

    final double start = buf.firstSampleTime();
    if (start < startTime) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return "FdsnTimeSimpleConstraint: " + J2kSec.toDateString(startTime) + ":"
        + J2kSec.toDateString(endTime);
  }


}
