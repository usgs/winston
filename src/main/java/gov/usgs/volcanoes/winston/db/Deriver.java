package gov.usgs.volcanoes.winston.db;

import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;

/**
 * A class to recalculate stored RSAM values
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class Deriver {
  private static final Logger LOGGER = LoggerFactory.getLogger(Deriver.class);

  private static final String DEFAULT_CONFIG_FILENAME = "Deriver.config";
  private static final double DEFAULT_CHUNK_SIZE = 600.0;

  private static final boolean DEFAULT_RSAM_ENABLE = true;
  private static final int DEFAULT_RSAM_DELTA = 10;
  private static final int DEFAULT_RSAM_DURATION = 60;

  private WinstonDatabase winston;
  private final InputEW input;
  private final Data data;

  private double startTime;
  private double endTime;

  private double chunkSize = 3600;

  private final boolean quit = false;

  private boolean rsamEnable = DEFAULT_RSAM_ENABLE;
  private int rsamDelta = DEFAULT_RSAM_DELTA;
  private int rsamDuration = DEFAULT_RSAM_DURATION;

  private final ConfigFile config;

  private List<String> sourceChannels;

  public Deriver() {
    config = new ConfigFile(DEFAULT_CONFIG_FILENAME);
    processConfigFile();
    input = new InputEW(winston);
    data = new Data(winston);
    deriveAll();
  }

  public void processConfigFile() {
    winston = WinstonDatabase.processWinstonConfigFile(config);

    sourceChannels = config.getList("channel");

    final String timeRange = config.getString("timeRange");
    double[] t;
    try {
      t = Time.parseTimeRange(timeRange);
      startTime = t[0];
      endTime = t[1];
    } catch (final ParseException e) {
      throw new RuntimeException("Can't parse timeRange");
    }

    chunkSize = StringUtils.stringToDouble(config.getString("chunkSize"), DEFAULT_CHUNK_SIZE);

    rsamEnable = StringUtils.stringToBoolean(config.getString("rsam.enable"), DEFAULT_RSAM_ENABLE);
    rsamDelta = StringUtils.stringToInt(config.getString("rsam.delta"), DEFAULT_RSAM_DELTA);
    rsamDuration =
        StringUtils.stringToInt(config.getString("rsam.duration"), DEFAULT_RSAM_DURATION);
  }

  public void deriveAll() {
    for (final String channel : sourceChannels) {
      LOGGER.info("Working on {}", channel);
      derive(channel, startTime, endTime);
    }
  }

  private void derive(final String ch, final double t1, final double t2) {
    input.setRowParameters((int) chunkSize + 65, 60);

    double ct = t1 - chunkSize;
    List<TraceBuf> tbs = null;
    while (ct < t2) {
      if (quit)
        break;
      ct += chunkSize;

      final double ret = Math.min(ct + chunkSize, t2);
      final CodeTimer netTimer = new CodeTimer("net");
      try {
        tbs = data.getTraceBufs(ch, ct, ret, 0);
      } catch (final UtilException e) {
      }
      netTimer.stop();

      if (tbs != null && tbs.size() > 0) {
        final CodeTimer inputTimer = new CodeTimer("input");
        input.rederive(tbs, rsamEnable, rsamDelta, rsamDuration);
        inputTimer.stop();

        LOGGER.info(String.format("Derived %d TraceBufs in %.3fms, insert completed in %.3fms",
            tbs.size(), netTimer.getRunTimeMillis(), inputTimer.getRunTimeMillis()));
      }
    }
  }

  public static void main(final String[] args) {
    new Deriver();
  }
}
