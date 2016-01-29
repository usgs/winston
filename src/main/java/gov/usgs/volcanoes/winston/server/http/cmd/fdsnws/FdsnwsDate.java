/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Format and parse a date. Should this subclass SimpleDateFormatter?
 * 
 * @author Tom Parker
 *
 */
public class FdsnwsDate {
  private static String DATE_STRING = "YYYY-MM-DD'T'HH:MM:SS.ss";
  
  /**
   * Format a date.
   * 
   * @param date date
   * @return string representation
   */
  public static String toString(Date date) {
    SimpleDateFormat sFormat = new SimpleDateFormat(DATE_STRING);
    return sFormat.format(date);
  }
  
  /**
   * Parse a date.
   * 
   * @param date string date
   * @return date object
   * @throws ParseException when provided a malformed string
   */
  public static Date parse(String date) throws ParseException {
    SimpleDateFormat sFormat = new SimpleDateFormat(DATE_STRING);
    return sFormat.parse(date);
  }
}
