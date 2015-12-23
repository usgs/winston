package gov.usgs.volcanoes.winston.server.httpCmd;

import java.util.Map;

public final class HttpConstants {
  public static final int ORDER_BY = 2;
  public static final String SORT_ORDER = "a";
  public static final String TIME_ZONE = "UTC";
  
  public static final String HELI_START_TIME = "-12";
  public static final String HELI_END_TIME = "now";
  public static final int HELI_HEIGHT = 800;
  public static final int HELI_WIDTH = 800;
  public static final int HELI_TIME_CHUNK = 30;
  public static final String HELI_BAR_RANGE = "auto";
  public static final boolean HELI_SHOW_CLIP = true;
  public static final boolean HELI_FORCE_CENTER = false;
  public static final boolean HELI_LABEL = false;
  public static final String HELI_CLIP_VALUE = "auto";
  
  private HttpConstants() {
  }
  
  public static void applyDefaults(Map<String, Object> map) {
    map.put("DEFAULT_ORDER_BY", new Integer(ORDER_BY));
    map.put("DEFAULT_SORT_ORDER", SORT_ORDER);
    map.put("DEFAULT_TIME_ZONE", TIME_ZONE);
    map.put("DEFAULT_T1", HELI_START_TIME);
    map.put("DEFAULT_T2", HELI_END_TIME);
    map.put("DEFAULT_H", HELI_HEIGHT);
    map.put("DEFAULT_W", HELI_WIDTH);
    map.put("DEFAULT_TC", HELI_TIME_CHUNK);
    map.put("DEFAULT_BR", HELI_BAR_RANGE);
    map.put("DEFAULT_SC", HELI_SHOW_CLIP);
    map.put("DEFAULT_FC", HELI_FORCE_CENTER);
    map.put("DEFAULT_LB", HELI_LABEL);
    map.put("DEFAULT_CV", HELI_CLIP_VALUE);
    map.put("DEFAULT_TZ", TIME_ZONE);  } 
}