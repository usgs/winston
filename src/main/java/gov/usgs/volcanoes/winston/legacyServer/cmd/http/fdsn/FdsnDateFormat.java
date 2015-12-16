package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import gov.usgs.volcanoes.core.time.Time;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnDateFormat extends SimpleDateFormat {

  public static final String FDSN_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSS";

  public FdsnDateFormat() {
    super(Time.FDSN_TIME_FORMAT);
  }

  @Override
  public Date parse(String source) throws ParseException {

    if (!source.contains("T"))
      source += "T00:00:00.0000";

    final int t = source.indexOf('T');
    if (source.length() == t + 8)
      source += ".0000";

    if (source.length() > t + 13)
      source = source.substring(0, t + 13);

    return super.parse(source);
  }

}
