package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.constraint;

import java.text.ParseException;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.FdsnDateFormat;

/**
 *
 * @author Tom Parker
 *
 */
abstract public class FdsnTimeConstraint implements FdsnConstraint {
  protected static final String FAR_IN_FUTURE = "2070-01-01T00:00:00.0000";
  protected static final String FAR_IN_PAST = "1970-01-01T00:00:00.0000";

  private FdsnDateFormat fdsnDate;
  
  protected FdsnTimeConstraint() {
    fdsnDate = new FdsnDateFormat();
  }
  
  protected double dateStringToDouble(final String s1, final String s2) throws ParseException {
    String s = StringUtils.stringToString(s1, s2);

    if (s.indexOf('T') == -1)
      s += "T00:00:00.0000";

    final int dot = s.indexOf('.');
    if (dot == -1)
      s += ".0000";
    else if (dot + 5 > s.length())
      s = s.substring(0, dot + 5);
    else
      while (dot + 5 < s.length())
        s += "0";

    return J2kSec.fromDate(fdsnDate.parse(s));
  }
}
