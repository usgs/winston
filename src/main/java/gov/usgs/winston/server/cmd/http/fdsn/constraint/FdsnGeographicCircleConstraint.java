package gov.usgs.winston.server.cmd.http.fdsn.constraint;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.proj.Projection;
import gov.usgs.util.Util;
import gov.usgs.winston.Channel;
import gov.usgs.winston.Instrument;

import java.awt.geom.Point2D;

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

    public FdsnGeographicCircleConstraint(String latitude, String longitude, String minRadius, String maxRadius) {
        double lat = Util.stringToDouble(latitude, DEFAULT_LATITUDE);
        double lon = Util.stringToDouble(longitude, DEFAULT_LONGITUDE);

        point = new Point2D.Double(lat, lon);

        this.minRadius = Util.stringToDouble(minRadius, DEFAULT_MINRADIUS);
        this.maxRadius = Util.stringToDouble(maxRadius, DEFAULT_MAXRADIUS);
    }

    public boolean matches(Channel chan) {
        Instrument i = chan.getInstrument();
        double lat = i.getLatitude();
        double lon = i.getLongitude();

        if (lat != Double.NaN && lon != Double.NaN) {
            Point2D.Double p = new Point2D.Double(lat, lon);
            double radius = Projection.distanceBetweenDegree(point, p);
            
            return (radius >= minRadius && radius <= maxRadius);
        } else
            return false;
    }

    public String toString() {
        return "FdsnGeographicCircleConstraint: " + point.x + "," + point.y + " " + minRadius + " >< " + maxRadius;
    }
}
