package gov.usgs.volcanoes.winston;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.render.HelicorderRenderer;
import gov.usgs.volcanoes.core.legacy.Arguments;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.winston.legacyServer.WWSClient;

/**
 * A command line driven program that connects to a WWS and plots a helicorder.
 * This could easily be programmatically extends to be some sort of Helicorder
 * Studio or other applications.
 *
 * @author Dan Cervelli
 */
public class PlotHelicorder {
  private String host;
  private int port;

  private final Settings settings = new Settings();

  private final Arguments args;

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat(Time.INPUT_TIME_FORMAT);

  public enum FileType {
    JPEG("jpg"), PNG("png"), PS("ps");

    private String extension;

    private FileType(final String s) {
      extension = s;
    }

    public static FileType fromExtenstion(String s) {
      final int i = s.lastIndexOf('.');
      if (i != -1)
        s = s.substring(s.indexOf('.'));

      for (final FileType m : FileType.values())
        if (m.getExtension() == s)
          return m;

      return null;
    }

    public String getExtension() {
      return extension;
    }
  }

  public PlotHelicorder(final String[] a) {
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    if (a.length == 0) {
      outputInstructions();
      System.exit(1);
    }

    final Set<String> flags = new HashSet<String>();
    final Set<String> kvs = new HashSet<String>();
    kvs.add("-wws");
    kvs.add("-s");
    kvs.add("-e");
    kvs.add("-m");
    kvs.add("-h");
    kvs.add("-tz");
    kvs.add("-to");
    kvs.add("-x");
    kvs.add("-y");
    kvs.add("-lm");
    kvs.add("-rm");
    kvs.add("-o");
    kvs.add("-c");
    kvs.add("-b");
    kvs.add("-r");
    kvs.add("-ft");
    args = new Arguments(a, flags, kvs);
    parseArgs();

    final WWSClient winston = new WWSClient(host, port);

    System.out.print("getting data...");
    final String[] scn = settings.station.split("_");
    final HelicorderData heliData = winston.getHelicorder(scn[0], scn[1], scn[2], scn[3],
        settings.startTime, settings.endTime, true);
    winston.close();
    System.out.println("done.");

    final HelicorderRenderer heliRenderer = new HelicorderRenderer();
    heliRenderer.setData(heliData);
    heliRenderer.setTimeChunk(settings.timeChunk);
    final Plot plot = new Plot();
    plot.setSize(settings.xSize, settings.ySize);

    plot.addRenderer(heliRenderer);
    heliRenderer.setLocation(settings.left, settings.top,
        settings.xSize - (settings.left + settings.right),
        settings.ySize - (settings.top + settings.bottom));
    double mean = heliData.getMeanMax();
    final double bias = heliData.getBias();
    mean = Math.abs(bias - mean);

    // auto-scale
    if (settings.clipValue == -1) {
      settings.clipValue = (int) (21 * mean);
      System.out.println("Automatic clip value: " + settings.clipValue);
    }
    if (settings.barRange == -1) {
      settings.barRange = (int) (3 * mean);
      System.out.println("Automatic bar range: " + settings.barRange);
    }
    heliRenderer.setHelicorderExtents(settings.startTime, settings.endTime,
        -1 * Math.abs(settings.barRange), Math.abs(settings.barRange));
    heliRenderer.setClipValue(settings.clipValue);
    heliRenderer.setShowClip(settings.showClip);
    heliRenderer.setTimeZoneAbbr(settings.timeZoneAbbr);
    heliRenderer.setTimeZoneOffset(settings.timeZoneOffset);
    heliRenderer.createDefaultAxis();

    System.out.print("writing...");
    try {
      switch (settings.fileType) {
        case JPEG:
          plot.writeJPEG(settings.outputFilename);
          break;
        case PNG:
          plot.writePNG(settings.outputFilename);
          break;
        case PS:
          plot.writePS(settings.outputFilename);
          break;
        default:
          System.out.println("I don't know how to write a " + settings.fileType + " file format.");
          break;
      }
      plot.writePNG(settings.outputFilename);
    } catch (final PlotException e) {
      e.printStackTrace();
    }
    System.out.println("done.");
  }

  public void outputInstructions() {
    System.out.println("Server/station/time options");
    System.out.println("-wws  [host]:[port], the WWS, mandatory argument");
    System.out.println("-s    station, mandatory argument");
    System.out.println("-e    end time [now], format: 'YYYYMMDDHHMMSS' (GMT) or 'now'");
    System.out.println("-m    minutes on x-axis [20]");
    System.out.println("-h    hours on y-axis [12]");
    System.out.println("-tz   time zone abbreviation [GMT]");
    System.out.println("-to   time zone offset, hours [0]");
    System.out.println();
    System.out.println("Output options");
    System.out.println("-x    total plot x-pixel size [1000]");
    System.out.println("-y    total plot y-pixel size [1000]");
    System.out.println("-lm   left margin pixels [70]");
    System.out.println("-rm   right margin pixels [70]");
    System.out.println("-o    output file name [heli.png]");
    System.out.println("-c    clip value, a number [auto]");
    System.out.println("-b    bar range, a number [auto]");
    System.out.println("-r    show clipped trace as red, 0 or 1 [0]");
  }

  class Settings {
    String station;
    double timeChunk = 20 * 60;
    double endTime;
    double startTime;

    int hours = 12;
    int xSize = 1000;
    int ySize = 1000;

    int left = 70;
    int top = 20;
    int right = 70;
    int bottom = 50;

    int clipValue = -1;
    int barRange = -1;
    boolean showClip = false;

    String timeZoneAbbr = "GMT";
    double timeZoneOffset = 0;

    String outputFilename = "heli.png";
    FileType fileType = FileType.fromExtenstion(outputFilename);
  }

  public void error(final String s) {
    System.out.println(s);
    try {
      Thread.sleep(10000);
    } catch (final InterruptedException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    System.exit(1);
  }

  public void parseArgs() {
    final String[] hp = args.get("-wws").split(":");
    if (hp.length != 2)
      error("You must specify a WWS as [host]:[port].");

    host = hp[0];
    if (host == null)
      error("You must specify a WWS host properly.");
    else
      System.out.println("host: " + host);

    port = -1;
    try {
      port = Integer.parseInt(hp[1]);
    } catch (final Exception e) {
    }
    if (port == -1)
      error("You must specify a WWS port properly.");
    else
      System.out.println("port: " + port);

    settings.station = args.get("-s");
    if (settings.station == null)
      error("You must specify a station with -s.");
    else
      System.out.println("station: " + settings.station);

    String s = args.get("-x");
    if (s != null) {
      int x = -1;
      try {
        x = Integer.parseInt(s);
      } catch (final Exception e) {
      }
      if (x == -1)
        error("Error with '-x' value.");
      System.out.println("x-pixels: " + x);
      settings.xSize = x;
    }

    s = args.get("-y");
    if (s != null) {
      int y = -1;
      try {
        y = Integer.parseInt(s);
      } catch (final Exception e) {
      }
      if (y == -1)
        error("Error with '-y' value.");
      System.out.println("y-pixels: " + y);
      settings.ySize = y;
    }

    s = args.get("-lm");
    if (s != null) {
      int lm = -1;
      try {
        lm = Integer.parseInt(s);
      } catch (final Exception e) {
      }
      if (lm == -1)
        error("Error with '-lm' value.");
      System.out.println("left margin: " + lm);
      settings.left = lm;
    }

    s = args.get("-rm");
    if (s != null) {
      int rm = -1;
      try {
        rm = Integer.parseInt(s);
      } catch (final Exception e) {
      }
      if (rm == -1)
        error("Error with '-rm' value.");
      System.out.println("right margin: " + rm);
      settings.right = rm;
    }

    s = args.get("-m");
    if (s != null) {
      int m = -1;
      try {
        m = Integer.parseInt(s);
      } catch (final Exception e) {
      }
      if (m == -1)
        error("Error with '-m' value.");
      System.out.println("x-axis minutes: " + m);
      settings.timeChunk = m * 60;
    }

    s = args.get("-h");
    if (s != null) {
      int h = -1;
      try {
        h = Integer.parseInt(s);
      } catch (final Exception e) {
      }
      if (h == -1)
        error("Error with '-h' value.");
      System.out.println("y-axis hours: " + h);
      settings.hours = h;
    }

    s = args.get("-o");
    if (s != null) {
      settings.outputFilename = s;
      System.out.println("output file: " + s);
    }

    s = args.get("-ft");
    if (s != null) {
      settings.fileType = FileType.fromExtenstion(s);
    }

    s = args.get("-tz");
    if (s != null) {
      settings.timeZoneAbbr = s;
      System.out.println("time zone abbreviation: " + s);
    }

    s = args.get("-to");
    if (s != null) {
      double h = Double.NaN;
      try {
        h = Double.parseDouble(s);
      } catch (final Exception e) {
      }
      if (Double.isNaN(h))
        error("Error with '-tzo' value.");
      System.out.println("time zone offset: " + h);
      settings.timeZoneOffset = h;
    }

    settings.endTime = J2kSec.now();
    s = args.get("-e");
    if (s != null) {
      if (!s.equals("now")) {
        Date date = null;
        try {
          date = dateFormat.parse(s);
        } catch (final Exception e) {
        }
        if (date == null)
          error("Error with '-e' value.");

        System.out.println("end time: " + s);
        settings.endTime = J2kSec.fromDate(date);
      }
    }

    s = args.get("-c");
    if (s != null) {
      int c = -1;
      try {
        c = Integer.parseInt(s);
      } catch (final Exception e) {
      }
      if (c == -1)
        error("Error with '-c' value.");
      System.out.println("clip value: " + c);
      settings.clipValue = c;
    }

    s = args.get("-b");
    if (s != null) {
      int b = -1;
      try {
        b = Integer.parseInt(s);
      } catch (final Exception e) {
      }
      if (b == -1)
        error("Error with '-b' value.");
      System.out.println("bar range: " + b);
      settings.barRange = b;
    }

    s = args.get("-r");
    if (s != null) {
      settings.showClip = s.equals("1");
      System.out.println("show clip: " + settings.showClip);
    }

    settings.startTime = settings.endTime - settings.hours * 60 * 60;
    settings.startTime -= settings.startTime % settings.timeChunk;
  }

  public static void main(final String[] args) {
    new PlotHelicorder(args);
  }

}
