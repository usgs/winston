package gov.usgs.volcanoes.winston.db;

import gov.usgs.volcanoes.core.data.Scnl;

public class DbUtils {

  public static final String scnlAsWinstonCode(Scnl scnl) {
    char delimiter = '$';
    StringBuilder sb = new StringBuilder();
    sb.append(scnl.station).append(delimiter).append(scnl.channel).append(delimiter)
        .append(scnl.network);
    if (!"--".equals(scnl.location)) {
      sb.append(delimiter).append(scnl.location);
    }
    
    return sb.toString();
  }
}
