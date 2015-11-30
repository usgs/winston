package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint;

import java.awt.geom.Point2D;

import gov.usgs.proj.Projection;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Instrument;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnGeographicCircleConstraint implements FdsnConstraint {

  private static double DEFAULT_LATITUDE = 0;
  private static double DEFAULT_LONGITUDE = 0;
  private static double DEFAULT_MINRADIUS = 0;
  private static double DEFAULT_MAXRADIUS = 180;

  Point2D.Double point;
  double minRadius;
  double maxRadius;

  public FdsnGeographicCircleConstraint(final String latitude, final String longitude,
      final String minRadius, final String maxRadius) {
    final double lat = Util.stringToDouble(latitude, DEFAULT_LATITUDE);
    final double lon = Util.stringToDouble(longitude, DEFAULT_LONGITUDE);

    point = new Point2D.Double(lat, lon);

    this.minRadius = Util.stringToDouble(minRadius, DEFAULT_MINRADIUS);
    this.maxRadius = Util.stringToDouble(maxRadius, DEFAULT_MAXRADIUS);
  }

  public boolean matches(final Channel chan) {
    final Instrument i = chan.getInstrument();
    final double lat = i.getLatitude();
    final double lon = i.getLongitude();

    if (lat != Double.NaN && lon != Double.NaN) {
      final Point2D.Double p = new Point2D.Double(lat, lon);
      final double radius = Projection.distanceBetweenDegree(point, p);

      return (radius >= minRadius && radius <= maxRadius);
    } else
      return false;
  }

  @Override
  public String toString() {
    return "FdsnGeographicCircleConstraint: " + point.x + "," + point.y + " " + minRadius + " >< "
        + maxRadius;
  }
}
