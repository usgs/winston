package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint;

import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Instrument;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.FdsnException;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnGeographicSquareConstraint implements FdsnConstraint {

  private static final double DEFAULT_MINLATITUDE = -90;
  private static final double DEFAULT_MAXLATITUDE = 90;
  private static final double DEFAULT_MINLONGITUDE = -180;
  private static final double DEFAULT_MAXLONGITUDE = 180;

  public final double minlatitude;
  public final double maxlatitude;
  public final double minlongitude;
  public final double maxlongitude;

  public FdsnGeographicSquareConstraint(final String minlatitude, final String maxlatitude,
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
