package gov.usgs.volcanoes.winston.server.http.cmd.fdsnws;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FdsnwsDate {
  private static String DATE_STRING = "YYYY-MM-DD'T'HH:MM:SS.ss";
  
  public static String toString(Date date) {
    SimpleDateFormat sFormat = new SimpleDateFormat(DATE_STRING);
    return sFormat.format(date);
  }
}
