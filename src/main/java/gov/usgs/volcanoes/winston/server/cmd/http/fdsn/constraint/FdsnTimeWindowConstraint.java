package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint;

import java.text.ParseException;

import gov.usgs.util.Util;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.FdsnException;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnTimeWindowConstraint extends FdsnTimeConstraint {

  double startBefore;
  double startAfter;
  double endBefore;
  double endAfter;

  public FdsnTimeWindowConstraint(final String startBefore, final String startAfter,
      final String endBefore, final String endAfter) throws FdsnException {
    super();
    try {
      this.startBefore = dateStringToDouble(startBefore, FAR_IN_FUTURE);
      this.startAfter = dateStringToDouble(startAfter, FAR_IN_PAST);
      this.endBefore = dateStringToDouble(endBefore, FAR_IN_FUTURE);
      this.endAfter = dateStringToDouble(endAfter, FAR_IN_PAST);
    } catch (final ParseException e) {
      throw new FdsnException(400, "Can't parse time constraint: " + this);
    }

  }

  public boolean matches(final Channel chan) {
    final double start = chan.getMinTime();
    if (start > startBefore || start < startAfter)
      return false;

    final double end = chan.getMaxTime();
    if (end > endBefore || end < endAfter)
      return false;

    return true;
  }

  @Override
  public String toString() {
    return "FdsnTimeWindowConstraint: " + Util.j2KToDateString(startBefore) + " : "
        + Util.j2KToDateString(startAfter) + " : " + Util.j2KToDateString(endBefore) + " : "
        + Util.j2KToDateString(endAfter);
  }
}
