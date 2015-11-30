package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint;

import java.text.DateFormat;

import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.FdsnDateFormat;

/**
 *
 * @author Tom Parker
 *
 */
public interface FdsnConstraint {

  public static final DateFormat DATE_FORMAT = new FdsnDateFormat();

  abstract boolean matches(Channel chan);
}
