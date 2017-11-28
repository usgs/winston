package gov.usgs.volcanoes.winston.in;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;

import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.legacy.Arguments;
import gov.usgs.volcanoes.core.legacy.ew.message.TraceBuf;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.Data;
import gov.usgs.volcanoes.winston.db.InputEW;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.in.ew.Options;


/**
 * A base class for writing a static data importer for Winston. Clients just
 * have to fill the instructions <code>StringBuffer</code>, implement
 * <code>readFile()</code> and call <code>process()</code>.
 *
 * <code>readFile()</code> is responsible for creating a map that maps Winston
 * compatible channel codes to lists of TRACEBUF-sized <code>Wave</code>s.
 *
 *
 * @author Dan Cervelli
 */
abstract public class StaticImporter {
  protected static StringBuffer instructions = new StringBuffer();

  protected WinstonDatabase winston;
  protected InputEW input;
  protected Channels channels;

  protected boolean rsamEnable = true;
  protected int rsamDelta = 10;
  protected int rsamDuration = 60;

  public StaticImporter() {
    setupWinston();
  }

  public void setupWinston() {
    final ConfigFile cf = new ConfigFile("Winston.config");
    final String driver = cf.getString("winston.driver");
    final String url = cf.getString("winston.url");
    final String db = cf.getString("winston.prefix");
    winston = new WinstonDatabase(driver, url, db);
    if (!winston.checkDatabase()) {
      throw new RuntimeException("Winston database does not exist and could not be created.");
    }
    input = new InputEW(winston);
    channels = new Channels(winston);
  }

  public void importMap(final Map<String, List<Wave>> map) {
    if (map == null) {
      System.out.println("Nothing to import.");
      return;
    }

    final CodeTimer timer = new CodeTimer("import");
    double minTime = 1E300;
    double maxTime = -1E300;

    for (final Iterator<Entry<String, List<Wave>>> iter = map.entrySet().iterator(); iter
        .hasNext();) {
      Entry<String, List<Wave>> entry = iter.next();
      String code = entry.getKey();

      if (!channels.channelExists(code)) {
        System.out.println("Creating new channel '" + code + "' in Winston.");
        channels.createChannel(code);
      }

      System.out.printf("Importing channel: %s.%n", code);
      List<Wave> waves = entry.getValue();
      System.out.printf("Converting %d waves into TraceBufs.%n", waves.size());
      final List<TraceBuf> tbs = new ArrayList<TraceBuf>(waves.size());

      final int maxSamples = (int) (Math.pow(2, 16) / 4);
      final Iterator<Wave> it = waves.iterator();
      final List<Wave> subWaves = new ArrayList<Wave>();
      while (it.hasNext()) {
        final Wave wave = it.next();
        if (wave.numSamples() > maxSamples) {
          subWaves.addAll(wave.split(maxSamples));
          it.remove();
        }
      }
      waves.addAll(subWaves);

      for (Wave wave : waves) {
        final TraceBuf tb = new TraceBuf(code, wave);
        minTime = Math.min(minTime, tb.firstSampleTime());
        maxTime = Math.max(maxTime, tb.lastSampleTime());
        tb.createBytes();
        tbs.add(tb);
        wave = null; // for garbage collection
      }
      waves = null;

      final int duration = (int) (maxTime - minTime);
      input.setRowParameters(duration + 5, Math.min(duration, 100));
      System.out.println("Writing TraceBufs to database.");
      input.inputTraceBufs(tbs, rsamEnable, rsamDelta, rsamDuration);

      System.out.println("Done.");
    }

    timer.stop();
    System.out.printf("Completed in %.2fs%n", timer.getRunTimeMillis() / 1000);
  }

  public static void process(final List<String> files, final StaticImporter impt) {
    if (files.size() == 0) {
      System.out.println("No files to import.");
    }

    final ListIterator<String> it = files.listIterator();
    while (it.hasNext()) {
      final String fn = it.next();
      System.out.println("Reading file: " + fn);

      // assume files really means files, not resource records --tjp
      final File f = new File(fn);
      if (f.isDirectory()) {
        File[] dFiles = f.listFiles();
        if (dFiles == null)
          continue;
        for (final File ff : dFiles) {

          it.add(fn + File.pathSeparatorChar + ff.getName());
        }
      } else {
        try {
          impt.importMap(impt.readFile(fn));
        } catch (final IOException e) {
          System.out.println("Can't read " + fn + " (" + e.getLocalizedMessage() + ")");
        }
      }
    }

  }

  protected Set<String> getArgumentSet() {
    final Set<String> kvs = new HashSet<String>();
    kvs.add("-rd");
    kvs.add("-rl");
    return kvs;
  }

  public void processArguments(final Arguments args) {
    final String rd = args.get("-rd");
    rsamDelta = StringUtils.stringToInt(rd, 10);
    final String rl = args.get("-rl");
    rsamDuration = StringUtils.stringToInt(rl, 60);
    System.out.printf("RSAM parameters: delta=%d, duration=%d.%n", rsamDelta, rsamDuration);
  }

  public void setRsamDelta(final int i) {
    rsamDelta = i;
  }

  public void setRsamDuration(final int i) {
    rsamDuration = i;
  }

  abstract public Map<String, List<Wave>> readFile(String fn) throws IOException;
}
