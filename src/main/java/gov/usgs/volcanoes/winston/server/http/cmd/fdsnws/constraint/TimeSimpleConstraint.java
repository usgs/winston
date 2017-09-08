/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnException;

/**
 * Constrain results to a simple time span.
 * 
 * @author Tom Parker
 *
 */
@SuppressWarnings("deprecation")
public class TimeSimpleConstraint extends TimeConstraint {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimeSimpleConstraint.class);

  public final double endTimeJ2k;
  public final double startTimeJ2k;

  /**
   * Constructor.
   * 
   * @param startTimeJ2k start time
   * @param endTimeJ2k end time
   * @throws FdsnException when things go wrong
   */
  public TimeSimpleConstraint(final String startTimeJ2k, final String endTimeJ2k)
      throws FdsnException {
    super();
    try {
      this.startTimeJ2k = dateStringToDouble(startTimeJ2k, FAR_IN_PAST);
      this.endTimeJ2k = dateStringToDouble(endTimeJ2k, FAR_IN_FUTURE);
    } catch (final ParseException e) {
      LOGGER.debug("Can't parse time constraint \"{}\"", this.toString());
      throw new FdsnException(400, "Can't parse time constraint: " + this.toString());
    }
  }

  public boolean matches(final Channel chan) {
    final double end = chan.getMaxTime();
    if (!Double.isNaN(end) && chan.getMaxTime() > endTimeJ2k) {
      return false;
    }

    final double start = chan.getMinTime();
    if (!Double.isNaN(start) && chan.getMinTime() < startTimeJ2k) {
      return false;
    }

    return true;
  }

  public boolean matches(final TraceBuf buf) {
    final double end = buf.lastSampleTime() + buf.samplingPeriod();
    if (end > endTimeJ2k) {
      return false;
    }

    final double start = buf.firstSampleTime();
    if (start < startTimeJ2k) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return "FdsnTimeSimpleConstraint: " + J2kSec.toDateString(startTimeJ2k) + ":"
        + J2kSec.toDateString(endTimeJ2k);
  }


}
