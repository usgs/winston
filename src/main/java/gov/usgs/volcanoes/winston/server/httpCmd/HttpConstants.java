package gov.usgs.volcanoes.winston.server.httpCmd;

import java.util.Map;

public final class HttpConstants {
  public static final String INPUT_DATE_FORMAT = "yyyyMMddHHmm";
  public static final String DISPLAY_DATE_FORMAT = "yyyy-MM-dd HH:mm";
  public final static int ONE_MINUTE_S = 60;
  public final static int ONE_HOUR_S = 60 * ONE_MINUTE_S;
  public final static int ONE_DAY_S = 24 * ONE_HOUR_S;

  public static final int ORDER_BY = 2;
  public static final String SORT_ORDER = "a";
  public static final String TIME_ZONE = "UTC";

  public static final String HELI_START_TIME = "-24";
  public static final String HELI_END_TIME = "now";
  public static final int HELI_HEIGHT = 800;
  public static final int HELI_WIDTH = 800;
  public static final int HELI_TIME_CHUNK = 30;
  public static final String HELI_BAR_RANGE = "auto";
  public static final boolean HELI_SHOW_CLIP = true;
  public static final boolean HELI_FORCE_CENTER = false;
  public static final boolean HELI_LABEL = false;
  public static final String HELI_CLIP_VALUE = "auto";

  public static final int RSAM_HEIGHT = 300;
  public static final int RSAM_WIDTH = 900;
  public static final boolean RSAM_DOWN_SAMPLE = false;
  public static final int RSAM_DOWN_SAMPLE_PERIOD = 0;
  public static final int RSAM_RSAM_PERIOD = 60;
  public static final boolean RSAM_DETREND = false;
  public static final String RSAM_START_TIME = "-12";
  public static final String RSAM_END_TIME = "now";
  public static final String RSAM_MAX = "auto";
  public static final String RSAM_MIN = "auto";
  public static final boolean RSAM_REMOVE_MEAN = false;
  public static final boolean RSAM_CSV = false;
  public static final int RSAM_REMOVE_MEAN_PERIOD = 300;

  public static final String GAPS_START_TIME = "-12";
  public static final String GAPS_END_TIME = "now";
  public static final int GAPS_MINIMUM_DURATION = 5;
  public static final boolean GAPS_WC = false;
  
  
  private HttpConstants() {}

  public static void applyDefaults(Map<String, Object> map) {
    map.put("DEFAULT_ORDER_BY", new Integer(ORDER_BY));
    map.put("DEFAULT_SORT_ORDER", SORT_ORDER);
    map.put("TIME_ZONE", TIME_ZONE);
    
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
    map.put("DEFAULT_TZ", TIME_ZONE);
    
    map.put("RSAM_HEIGHT", RSAM_HEIGHT);
    map.put("RSAM_WIDTH", RSAM_WIDTH);
    map.put("RSAM_DOWN_SAMPLE", RSAM_DOWN_SAMPLE);
    map.put("RSAM_DOWN_SAMPLE_PERIOD", RSAM_DOWN_SAMPLE_PERIOD);
    map.put("RSAM_RSAM_PERIOD", RSAM_RSAM_PERIOD);
    map.put("RSAM_DETREND", RSAM_DETREND);
    map.put("RSAM_START_TIME", RSAM_START_TIME);
    map.put("RSAM_END_TIME", RSAM_END_TIME);
    map.put("RSAM_MAX", RSAM_MAX);
    map.put("RSAM_MIN", RSAM_MIN);
    map.put("RSAM_REMOVE_MEAN", RSAM_REMOVE_MEAN);
    map.put("RSAM_CSV", RSAM_CSV);
    map.put("RSAM_REMOVE_MEAN_PERIOD", RSAM_REMOVE_MEAN_PERIOD);
    
    map.put("GAPS_START_TIME", GAPS_START_TIME);
    map.put("GAPS_END_TIME", GAPS_END_TIME);
    map.put("GAPS_MINIMUM_DURATION", GAPS_MINIMUM_DURATION);
    map.put("GAPS_WC", GAPS_WC);

  }
}
