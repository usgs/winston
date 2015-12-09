package gov.usgs.volcanoes.winston.in.ew;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gov.usgs.earthworm.Menu;
import gov.usgs.earthworm.WaveServer;
import gov.usgs.earthworm.message.TraceBuf;
import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.InputEW;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

/**
 * If a span is added to the job it is an explicit guarantee that there is
 * no existing data in that span. Therefore incoming TraceBufs only need to be
 * between the given span to avoid overlappers.
 *
 * @author Dan Cervelli
 */
public class ImportWSJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImportWSJob.class);

  private final WinstonDatabase winston;
  private final WaveServer waveServer;

  private String channel;
  private final List<double[]> spans;

  private final Menu menu;

  private final Channels channels;
  private final InputEW input;

  private double chunkSize;
  private int chunkDelay;

  private boolean rsamEnable = true;
  private int rsamDelta = 10;
  private int rsamDuration = 60;

  private boolean quit = false;

  private boolean requestSCNL = false;

  private final ImportWS importWS;

  public ImportWSJob(final WinstonDatabase w, final WaveServer ws, final ImportWS is) {
    importWS = is;
    winston = w;
    waveServer = ws;
    spans = new ArrayList<double[]>(10);
    channels = new Channels(winston);
    input = new InputEW(winston);
    menu = importWS.getMenu();
    requestSCNL = importWS.getRequestSCNL();
  }

  public void setRSAMParameters(final boolean en, final int rd, final int rl) {
    rsamEnable = en;
    rsamDelta = rd;
    rsamDuration = rl;
  }

  public void setChannel(final String c) {
    channel = c;
  }

  public String getChannel() {
    return channel;
  }

  public void addSpan(final double t1, final double t2) {
    spans.add(new double[] {t1, t2});
  }

  public void setChunkSize(final double sec) {
    chunkSize = sec;
  }

  public void setChunkDelay(final int ms) {
    chunkDelay = ms;
  }

  public void quit() {
    quit = true;
  }

  private void getData(final double[] span) {
    try {
      final String[] ss = channel.split("\\$");
      String loc = null;
      if (ss.length == 4)
        loc = ss[3];

      if (requestSCNL && loc == null)
        loc = "--";

      final double t1 = span[0];
      final double t2 = span[1];

      LOGGER.info("{}: downloading gap: [{} -> {}, {}]", channel, J2kSec.toDateString(span[0]),
          J2kSec.toDateString(span[1]), Time.secondsToString(span[1] - span[0]));

      input.setRowParameters((int) chunkSize + 65, 60);

      double ct = t1 - chunkSize;
      List<TraceBuf> tbs = null;
      final CodeTimer timer = new CodeTimer("chunk");
      int total = 0;
      double totalInsTime = 0;
      double totalDlTime = 0;
      while (ct < t2) {
        if (quit)
          break;
        ct += chunkSize;
        final double ret = Math.min(ct + chunkSize + 5, t2 + 5);
        final CodeTimer netTimer = new CodeTimer("net");
        tbs = waveServer.getTraceBufs(ss[0], ss[1], ss[2], loc, J2kSec.asEpoch(ct - 5),
            J2kSec.asEpoch(ret));
        netTimer.stop();
        totalDlTime += netTimer.getTotalTimeMillis();
        if (tbs != null && tbs.size() > 0) {
          final Iterator<TraceBuf> it = tbs.iterator();
          double minTime = 1E300;
          double maxTime = -1E300;
          while (it.hasNext()) {
            final TraceBuf tb = it.next();
            minTime = Math.min(tb.getStartTimeJ2K(), minTime);
            maxTime = Math.max(tb.getEndTimeJ2K(), maxTime);
            if (tb.getEndTimeJ2K() < t1 || tb.getStartTimeJ2K() > t2) {
              // these are totally outside range so can be dropped quietly.
              it.remove();
              continue;
            }
            if (tb.getStartTimeJ2K() - t1 < -0.0001 || tb.getEndTimeJ2K() - t2 > 0.0001) {
              it.remove();
              LOGGER.debug("Overlapping TraceBuf skipped. {} - {}", tb.getStartTimeJ2K() - t1,
                  tb.getEndTimeJ2K() - t2);
              continue;
            }
            tb.createBytes();
          }
          if (tbs.size() == 0)
            continue;
          try {
            if (chunkDelay > 0) {
              LOGGER.debug("{}: delaying for {}ms...", channel, chunkDelay);
              Thread.sleep(chunkDelay);
            }
          } catch (final Exception e) {
            e.printStackTrace();
          }
          final CodeTimer inputTimer = new CodeTimer("input");
          final List<InputEW.InputResult> results =
              input.inputTraceBufs(tbs, rsamEnable, rsamDelta, rsamDuration);
          inputTimer.stop();
          totalInsTime += inputTimer.getTotalTimeMillis();
          LOGGER.debug("{}: {} tb ({}/{}ms), [{} -> {}, {}]", channel, tbs.size(),
              netTimer.getRunTimeMillis(), inputTimer.getRunTimeMillis(),
              J2kSec.toDateString(minTime), J2kSec.toDateString(maxTime),
              Time.secondsToString(maxTime - minTime));

          // TODO: clean this up, unify with ImportEW
          if (results.size() == 1) {
            // TODO: handle errors
            final InputEW.InputResult result = results.get(0);
            LOGGER.warn("Error: {}", result.code);
          } else {
            for (int i = 0; i < results.size() - 2; i++) {
              final InputEW.InputResult result = results.get(i);
              final TraceBuf tb = result.traceBuf;
              switch (result.code) {
                case SUCCESS_CREATED_TABLE:
                  LOGGER.info("{}: day table created ({})", channel,
                      J2kSec.format("yyyy-MM-dd", tb.getEndTimeJ2K() + 1));
                case SUCCESS:
                  total++;
                  LOGGER.debug("Insert: {}", tb.toString());
                  break;
                case ERROR_DATABASE:
                  // fixing
                  LOGGER.debug("Database error: {}", tb.toString());
                  break;
                case ERROR_UNKNOWN:
                  LOGGER.warn("Unknown insert error: {}", tb.toString());
                  break;
                case ERROR_CHANNEL:
                case ERROR_NULL_TRACEBUF:
                  // these errors should never occur
                  LOGGER.warn("Bad channel/null TraceBuf.");
                  break;
                case ERROR_DUPLICATE:
                  LOGGER.info("Duplicate TraceBuf: {}", tb.toString());
                  break;
                case NO_CODE:
                  // this should never occur
                  LOGGER.warn("No error/success code: {}", tb.toString());
                  break;
                case ERROR_HELICORDER:
                  break;
                case ERROR_INPUT:
                  break;
                case ERROR_NO_WINSTON:
                  break;
                case ERROR_TIME_SPAN:
                  break;
                case SUCCESS_HELICORDER:
                  break;
                case SUCCESS_TIME_SPAN:
                  break;
                default:
                  break;
              }
            }
          }
        }
      }
      timer.stop();
      importWS.addStats(total, totalDlTime, totalInsTime);
      LOGGER.info("{}: gap {}, {} tbs inserted in {}ms ({}ms/tb)", channel,
          (quit ? "interrupted" : "finished"), total, timer.getTotalTimeMillis(),
          (total == 0 ? 0 : timer.getTotalTimeMillis() / total));
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

  private void getAllData() {
    for (final double[] span : spans) {
      if (!quit)
        getData(span);
    }
  }

  public double getSpansDuration() {
    double duration = 0.0;
    for (final double[] span : spans)
      duration += span[1] - span[0];
    return duration;
  }

  public void go() {
    for (final double[] span : spans) {
      LOGGER.info(String.format("%s: gap: [%s -> %s, %s]", channel, J2kSec.toDateString(span[0]),
          J2kSec.toDateString(span[1]), Time.secondsToString(span[1] - span[0])));
    }

    LOGGER.info(
        "{}: starting job, total gaps: {} for a total duration of {}, Chunk: {}s, Delay: {}ms",
        channel, spans.size(), Time.secondsToString(getSpansDuration()), chunkSize,
        chunkDelay);

    if (!menu.channelExists(channel)) {
      LOGGER.error("Channel does not exist on source WaveServer.");
      return;
    }

    if (!channels.channelExists(channel)) {
      LOGGER.info("Creating new channel '{}' in Winston database.", channel);
      channels.createChannel(channel);
    }

    waveServer.connect();
    getAllData();
    waveServer.close();
    spans.clear();
  }
}
