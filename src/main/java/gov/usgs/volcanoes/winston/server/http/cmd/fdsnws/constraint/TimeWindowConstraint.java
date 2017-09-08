/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import java.text.ParseException;
import java.util.Map;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnException;

/**
 * Constrain results to a time window.
 * 
 * @author Tom Parker
 *
 */
@SuppressWarnings("deprecation")
public class TimeWindowConstraint extends TimeConstraint {

  private double startBefore;
  private double startAfter;
  private double endBefore;
  private double endAfter;

  /**
   * Constructor.
   * 
   * @param startBefore latest start
   * @param startAfter earliest start
   * @param endBefore latest end
   * @param endAfter earliest end
   * @throws FdsnException when things go wrong
   */
  public TimeWindowConstraint(final String startBefore, final String startAfter,
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
    TimeSpan timeSpan = chan.timeSpan;
    final double start = timeSpan.startTime;
    if (start > startBefore || start < startAfter)
      return false;

    final double end = timeSpan.endTime;
    if (end > endBefore || end < endAfter)
      return false;

    return true;
  }

  @Override
  public String toString() {
    return "FdsnTimeWindowConstraint: " + J2kSec.toDateString(startBefore) + " : "
        + J2kSec.toDateString(startAfter) + " : " + J2kSec.toDateString(endBefore) + " : "
        + J2kSec.toDateString(endAfter);
  }

  public static TimeWindowConstraint build(Map<String, String> arguments) throws FdsnException {
    final String startBefore = arguments.get("startbefore");
    final String startAfter = arguments.get("startafter");
    final String endBefore = arguments.get("endbefore");
    final String endAfter = arguments.get("andafter");
    return new TimeWindowConstraint(startBefore, startAfter, endBefore, endAfter);
  }


}
