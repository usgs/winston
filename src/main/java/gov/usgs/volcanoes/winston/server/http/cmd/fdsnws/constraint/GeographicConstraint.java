package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import java.util.Map;

import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnException;

public abstract class GeographicConstraint extends FdsnConstraint {

  public static GeographicConstraint build (Map<String, String> arguments) throws FdsnException {
    final String latitude = getArg(arguments, "latitude", "lat");
    final String longitude = getArg(arguments, "longitude", "lon");
    final String minRadius = arguments.get("minradius");
    final String maxRadius = arguments.get("maxradius");
    
    if (!(latitude == null && longitude == null && minRadius == null && maxRadius == null)) {
      return new GeographicCircleConstraint(latitude, longitude, minRadius, maxRadius);
    } else {
      final String minLatitude = getArg(arguments, "minlatitude", "minLat");
      final String maxLatitude = getArg(arguments, "maxlatitude", "maxLat");
      final String minLongitude = getArg(arguments, "minlongitude", "minLon");
      final String maxLongitude = getArg(arguments, "maxlongitude", "maxLon");

      return new GeographicSquareConstraint(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }
  }
}
