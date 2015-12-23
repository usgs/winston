package gov.usgs.volcanoes.winston.server.httpCmd;

import java.util.Map;

public final class HttpConstants {
  public static int DEFAULT_ORDER_BY = 2;
  public static String DEFAULT_SORT_ORDER = "a";
  public static String DEFAULT_TIME_ZONE = "UTC";

  private HttpConstants() {
  }
  
  public static void applyDefaults(Map<String, Object> map) {
    map.put("DEFAULT_ORDER_BY", new Integer(DEFAULT_ORDER_BY));
    map.put("DEFAULT_SORT_ORDER", DEFAULT_SORT_ORDER);
    map.put("DEFAULT_TIME_ZONE", DEFAULT_TIME_ZONE);
  }
  
}
