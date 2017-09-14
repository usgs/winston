/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.constraint;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.server.http.cmd.fdsnws.FdsnwsDate;

/**
 * Constrain results by time.
 * 
 * @author Tom Parker
 *
 */
@SuppressWarnings("unused")
abstract public class TimeConstraint extends FdsnConstraint {
  private static final Logger LOGGER = LoggerFactory.getLogger(TimeConstraint.class);

  protected static final String FAR_IN_FUTURE = "2070-01-01T00:00:00.0000";
  protected static final String FAR_IN_PAST = "1970-01-01T00:00:00.0000";

  protected double dateStringToDouble(final String s1, final String s2) throws ParseException {
    String s = StringUtils.stringToString(s1, s2);
    if (s.indexOf('T') == -1) {
      s += "T00:00:00.0000";
    } else {
      final int dot = s.indexOf('.');
      if (dot == -1) {
        s += ".0000";
      } else if (dot + 5 > s.length()) {
        s = s.substring(0, dot + 5);
      } else {
        StringBuffer sb = new StringBuffer();
        sb.append(s);
        while (dot + 5 > sb.length()) {
          sb.append("0");
        }
        s = sb.toString();

      }
    }
    return J2kSec.fromDate(FdsnwsDate.parse(s));
  }
}
