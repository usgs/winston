/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.http;

import java.util.Map;

/**
 * Constants used for HTTP commands.
 * 
 * @author Tom Parker
 *
 */
public final class HttpConstants {
  /** Date format used for user input */
  public static final String INPUT_DATE_FORMAT = "yyyyMMddHHmm";
  
  /** date format used for display */
  public static final String DISPLAY_DATE_FORMAT = "yyyy-MM-dd HH:mm";
  
  /** one minute in seconds*/
  public final static int ONE_MINUTE_S = 60;
  
  /** one hour in seconds */
  public final static int ONE_HOUR_S = 60 * ONE_MINUTE_S;
  
  /** one day in seconds */
  public final static int ONE_DAY_S = 24 * ONE_HOUR_S;

  /** default column for sorting menu */
  public static final int ORDER_BY = 2;
  
  /** default menu sort order */
  public static final String SORT_ORDER = "a";
  
  /** default time zone */
  public static final String TIME_ZONE = "UTC";

  /** default heli start time */
  public static final String HELI_START_TIME = "-24";
  
  /** default heli end time */
  public static final String HELI_END_TIME = "now";
  
  /** default heli height */
  public static final int HELI_HEIGHT = 800;
  
  /** default heli width */
  public static final int HELI_WIDTH = 800;
  
  /** default heli row length */
  public static final int HELI_TIME_CHUNK = 30;
  
  /** default heli row height */
  public static final String HELI_BAR_RANGE = "auto";
  
  /** if true highlight clipped traces */
  public static final boolean HELI_SHOW_CLIP = true;
  
  /** if true center heli trace */
  public static final boolean HELI_FORCE_CENTER = false;
  
  /** if true apply channel label to plot */
  public static final boolean HELI_LABEL = false;
  
  /** default clipping value */
  public static final String HELI_CLIP_VALUE = "auto";

  /** height of RSAM plot */
  public static final int RSAM_HEIGHT = 300;
  
  /** width of RSAM plot */
  public static final int RSAM_WIDTH = 900;
  
  /** if true, down sample RSAM */
  public static final boolean RSAM_DOWN_SAMPLE = false;
  
  /** default RSAM down sample period */
  public static final int RSAM_DOWN_SAMPLE_PERIOD = 0;
  
  /** rsam period */
  public static final int RSAM_RSAM_PERIOD = 60;
  
  /** if true, detrend rsam plots */
  public static final boolean RSAM_DETREND = false;
  
  /** default rsam start time */
  public static final String RSAM_START_TIME = "-12";
  
  /** rsam end time */
  public static final String RSAM_END_TIME = "now";
  
  /** rsam max value */
  public static final String RSAM_MAX = "auto";
  
  /** rsam min value */
  public static final String RSAM_MIN = "auto";
  
  /** if true remove mean before plotting */
  public static final boolean RSAM_REMOVE_MEAN = false;
  
  /** if true return CSV rather than a file */
  public static final boolean RSAM_CSV = false;
  
  /** period used for de-mean filter */
  public static final int RSAM_REMOVE_MEAN_PERIOD = 300;

  /** start time for gaps report */
  public static final String GAPS_START_TIME = "-12";
  
  /** end time for gaps report */
  public static final String GAPS_END_TIME = "now";
  
  /** Minimum duration to declare a gap */
  public static final int GAPS_MINIMUM_DURATION = 5;
  
  /** if true write terse report */
  public static final boolean GAPS_WC = false;
  
  
  private HttpConstants() {}

  /**
   * Add the defaults to a Map.
   * 
   * @param map map to be populated
   */
  public static void applyDefaults(Map<String, Object> map) {
    map.put("DEFAULT_ORDER_BY", Integer.valueOf(ORDER_BY));
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
