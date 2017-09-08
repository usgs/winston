/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Instrument;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnException;

/**
 * Constrain results to a bounding box.
 * 
 * @author Tom Parker
 *
 */
@SuppressWarnings("deprecation")
public class GeographicSquareConstraint extends GeographicConstraint {

  private static final double DEFAULT_MINLATITUDE = -90;
  private static final double DEFAULT_MAXLATITUDE = 90;
  private static final double DEFAULT_MINLONGITUDE = -180;
  private static final double DEFAULT_MAXLONGITUDE = 180;

  private final double minlatitude;
  private final double maxlatitude;
  private final double minlongitude;
  private final double maxlongitude;

  /**
   * Constructor.
   * 
   * @param minlatitude minimum latitude in decimal degrees
   * @param maxlatitude maximum latitude in decimal degrees
   * @param minlongitude minimum longitude in decimal degrees
   * @param maxlongitude maximum longitude in decimal degrees
   * @throws FdsnException when things go wrong
   */
  public GeographicSquareConstraint(final String minlatitude, final String maxlatitude,
      final String minlongitude, final String maxlongitude) throws FdsnException {

    this.minlatitude = StringUtils.stringToDouble(minlatitude, DEFAULT_MINLATITUDE);
    this.maxlatitude = StringUtils.stringToDouble(maxlatitude, DEFAULT_MAXLATITUDE);
    this.minlongitude = StringUtils.stringToDouble(minlongitude, DEFAULT_MINLONGITUDE);
    this.maxlongitude = StringUtils.stringToDouble(maxlongitude, DEFAULT_MAXLONGITUDE);

  }

  public boolean matches(final Channel chan) {
    final Instrument i = chan.getInstrument();

    if (i.getLatitude() < minlatitude)
      return false;

    if (i.getLatitude() > maxlatitude)
      return false;

    if (i.getLongitude() < minlongitude)
      return false;

    if (i.getLongitude() > maxlongitude)
      return false;

    return true;
  }

  @Override
  public String toString() {
    return "FdsnGeographcSquareConstraint" + minlatitude + ", " + minlongitude + " -> "
        + maxlatitude + ", " + maxlongitude;
  }
}
