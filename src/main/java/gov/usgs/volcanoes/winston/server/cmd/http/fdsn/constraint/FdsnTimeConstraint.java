package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint;

import gov.usgs.util.Util;

import java.text.ParseException;

/**
 * 
 * @author Tom Parker
 * 
 */
abstract public class FdsnTimeConstraint implements FdsnConstraint {
    protected static final String FAR_IN_FUTURE = "2070-01-01T00:00:00.0000";
    protected static final String FAR_IN_PAST = "1970-01-01T00:00:00.0000";

    protected double dateStringToDouble(String s1, String s2) throws ParseException {
        String s = Util.stringToString(s1, s2);
        
        if (s.indexOf('T') == -1) 
            s += "T00:00:00.0000";
        
        int dot = s.indexOf('.');
        if (dot == -1)
            s += ".0000";
        else if (dot + 5 > s.length())
            s = s.substring(0, dot + 5);
        else
            while (dot + 5 < s.length())
                s += "0";
        
        return Util.dateToJ2K(DATE_FORMAT.parse(s));
    }
}
