package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.constraint;

import gov.usgs.volcanoes.winston.Channel;

/**
 *
 * @author Tom Parker
 *
 */
public interface FdsnConstraint {

//  public static final DateFormat DATE_FORMAT = new FdsnDateFormat();

  abstract boolean matches(Channel chan);
}
