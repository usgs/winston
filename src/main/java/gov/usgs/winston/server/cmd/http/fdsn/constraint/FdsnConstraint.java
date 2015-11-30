package gov.usgs.winston.server.cmd.http.fdsn.constraint;

import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.winston.Channel;
import gov.usgs.winston.server.cmd.http.fdsn.FdsnDateFormat;

import java.text.DateFormat;

/**
 * 
 * @author Tom Parker
 *
 */
public interface  FdsnConstraint {

    public static final DateFormat DATE_FORMAT = new FdsnDateFormat();

    abstract boolean matches(Channel chan);
}
