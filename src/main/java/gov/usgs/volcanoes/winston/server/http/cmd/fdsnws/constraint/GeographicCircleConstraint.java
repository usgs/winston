/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import java.awt.geom.Point2D;

import gov.usgs.volcanoes.core.math.proj.Projection;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Instrument;

/**
 * Constrain results to a geographical circle.
 * 
 * @author Tom Parker
 *
 */
public class GeographicCircleConstraint extends GeographicConstraint {

  private static double DEFAULT_LATITUDE = 0;
  private static double DEFAULT_LONGITUDE = 0;
  private static double DEFAULT_MINRADIUS = 0;
  private static double DEFAULT_MAXRADIUS = 180;

  private Point2D.Double point;
  private double minRadius;
  private double maxRadius;

  /**
   * Constructor.
   * 
   * @param latitude decimal latitude
   * @param longitude decimal longitude
   * @param minRadius min radius in degrees
   * @param maxRadius max radius in degrees
   */
  public GeographicCircleConstraint(final String latitude, final String longitude,
      final String minRadius, final String maxRadius) {
    final double lat = StringUtils.stringToDouble(latitude, DEFAULT_LATITUDE);
    final double lon = StringUtils.stringToDouble(longitude, DEFAULT_LONGITUDE);

    point = new Point2D.Double(lat, lon);

    this.minRadius = StringUtils.stringToDouble(minRadius, DEFAULT_MINRADIUS);
    this.maxRadius = StringUtils.stringToDouble(maxRadius, DEFAULT_MAXRADIUS);
  }

  public boolean matches(final Channel chan) {
    final Instrument i = chan.instrument;
    final double lat = i.latitude;
    final double lon = i.longitude;

    if (Double.isNaN(lat) || Double.isNaN(lon)) {
      return false;
    }

    final Point2D.Double p = new Point2D.Double(lat, lon);
    final double radius = Projection.distanceBetweenDegree(point, p);

    return (radius >= minRadius && radius <= maxRadius);
  }

  @Override
  public String toString() {
    return "FdsnGeographicCircleConstraint: " + point.x + "," + point.y + " " + minRadius + " >< "
        + maxRadius;
  }
}
