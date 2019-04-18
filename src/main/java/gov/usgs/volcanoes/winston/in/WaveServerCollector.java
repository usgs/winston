package gov.usgs.volcanoes.winston.in;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.legacy.ew.Menu;
import gov.usgs.volcanoes.core.legacy.ew.MenuItem;
import gov.usgs.volcanoes.core.legacy.ew.SCN;
import gov.usgs.volcanoes.core.legacy.ew.WaveServer;
import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.Input;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;

/**
 *
 * @author Dan Cervelli
 */
public class WaveServerCollector extends Thread {
  public static final int COLLECT = 1;
  public static final int FILL_GAPS = 2;
  private final int mode;
  private final WinstonDatabase winston;
  private final Channels winstonStations;
  private final Input input;
  private final Data data;
  private final int interval; // seconds
  private final int maxSize; // seconds
  private final int delay;

  private final List<SCN> channels;
  private final String name;
  private final WaveServer waveServer;

  public WaveServerCollector(final String n, final WinstonDatabase w, final WaveServer ws,
      final int i, final int m, final int d, final int md) {
    name = n;
    winston = w;
    input = new Input(winston);
    winstonStations = new Channels(winston);
    data = new Data(winston);
    interval = i;
    maxSize = m;
    delay = d;
    mode = md;
    waveServer = ws;
    channels = new ArrayList<SCN>();
  }

  public void startCollecting() {
    start();
  }

  public void stopCollecting() {

  }

  public void addStation(final SCN ci) {
    channels.add(ci);
  }

  public void fillGap(final SCN scn, final double t1, final double t2) {
    double ct = t1;
    waveServer.connect();
    while (ct < t2) {
      final long ts = System.currentTimeMillis();
      List<TraceBuf> tbs = null;
      if (t2 - ct > maxSize) {
        tbs = waveServer.getTraceBufs(scn.station, scn.channel, scn.network, Time.j2kToEw(ct - 5),
            Time.j2kToEw(ct + maxSize + 5));
        ct += maxSize;
      } else {
        tbs = waveServer.getTraceBufs(scn.station, scn.channel, scn.network, Time.j2kToEw(ct - 5),
            Time.j2kToEw(t2 + 5));
        ct = t2;
      }
      if (tbs != null && tbs.size() > 0) {
        for (final Object o : tbs) {
          final TraceBuf tb = (TraceBuf) o;
          tb.createBytes();
          input.inputTraceBuf(tb, false);
        }
      }
      final long te = System.currentTimeMillis();
      System.out.println("Chunk: " + ((double) (te - ts) / 1000) + "s");
    }
    waveServer.close();
  }

  public void fillGaps() {
    final Menu menu = waveServer.getMenu();
    final Iterator<SCN> it = channels.iterator();
    while (it.hasNext()) {
      final SCN scn = it.next();
      final String code = scn.toString().replace('_', '$');
      System.out.println("[" + name + "/" + code + "]: ");

      final double now = Ew.now();

      final MenuItem mi = menu.getItem(scn);
      if (mi == null)
        continue;

      Scnl scnl = null;
      try {
        scnl = Scnl.parse(code, "_");
      } catch (UtilException e) {
        System.err.println("Cannot parse code: " + code);
        e.printStackTrace();
      }
      final List<double[]> gaps = data.findGaps(scnl, Time.ewToj2k(mi.startTime), now);
      final double[] span = data.getTimeSpan(code);
      final double fdt = span[0];
      if (fdt > Time.ewToj2k(mi.startTime)) {
        gaps.add(new double[] {Time.ewToj2k(mi.startTime), fdt});
      }
      if (gaps != null) {
        final Iterator<double[]> it2 = gaps.iterator();
        while (it2.hasNext()) {
          final double[] gap = it2.next();
          System.out.println((gap[1] - gap[0]) + "s, " + gap[0] + " -> " + gap[1]);
          fillGap(scn, gap[0] - 5, gap[1] + 5);
        }
      }
    }
  }

  public void collect() {
    final Iterator<SCN> it = channels.iterator();
    while (it.hasNext()) {
      final long ts = System.currentTimeMillis();
      final SCN scn = it.next();
      final String code = scn.toString().replace('_', '$');
      System.out.print("[" + name + "/" + code + "]: ");

      boolean stationOk = true;
      if (!winstonStations.channelExists(code)) {
        System.out.print("creating new station in Winston; ");
        stationOk = winstonStations.createChannel(code);
      }
      if (!stationOk)
        continue;

      final double[] span = data.getTimeSpan(code);
      final double now = Ew.now();
      double last = Time.j2kToEw(span[1]);
      if (last == -1E300)
        last = now - 10 * 60;

      final List<TraceBuf> tbs =
          waveServer.getTraceBufs(scn.station, scn.channel, scn.network, last, now);
      if (tbs == null || tbs.size() == 0) {
        System.out.print("wave server returned no data; ");
      } else {
        for (final Object o : tbs) {
          final TraceBuf tb = (TraceBuf) o;
          tb.createBytes();
          input.inputTraceBuf(tb, false);
        }
      }

      final long te = System.currentTimeMillis();

      System.out.println("done. " + ((double) (te - ts) / 1000) + "s");
    }
  }

  @Override
  public void run() {
    try {
      Thread.sleep(delay * 1000l);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (mode == COLLECT) {
      while (true) {
        collect();
        try {
          Thread.sleep(interval * 1000l);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    } else {
      fillGaps();
    }
  }
}
