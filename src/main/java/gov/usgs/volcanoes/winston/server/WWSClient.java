package gov.usgs.volcanoes.winston.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import gov.usgs.earthworm.WaveServer;
import gov.usgs.net.ReadListener;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.data.file.FileType;
import gov.usgs.plot.data.file.SeismicDataFile;
import gov.usgs.util.Arguments;
import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.Retriable;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;

/**
 * A class that extends the Earthworm Wave Server to include a get helicorder
 * function for WWS.
 *
 * @author Dan Cervelli
 */
public class WWSClient extends WaveServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(WWSClient.class);

  protected ReadListener readListener;

  public WWSClient(final String h, final int p) {
    super(h, p);
    setTimeout(60000);
  }

  public void setReadListener(final ReadListener rl) {
    readListener = rl;
  }

  public int getProtocolVersion() {
    int version = 1;
    try {
      if (!connected())
        connect();

      socket.setSoTimeout(1000);
      writeString("VERSION\n");
      final String result = readString();
      version = Integer.parseInt(result.split(" ")[1]);
    } catch (final Exception e) {
    } finally {
      try {
        socket.setSoTimeout(timeout);
      } catch (final Exception e) {
      }
    }
    return version;
  }

  protected byte[] getData(final String req, final boolean compressed) {
    byte[] ret = null;
    final Retriable<byte[]> rt = new Retriable<byte[]>("WWSClient.getData()", maxRetries) {
      @Override
      public void attemptFix() {
        close();
      }

      @Override
      public boolean attempt() throws UtilException {
        try {
          if (!connected())
            connect();

          writeString(req);
          final String info = readString();

          if (info.startsWith("ERROR")) {
            logger.warning("Sent: " + req);
            logger.warning("Got: " + info);
            return false;
          }

          final String[] ss = info.split(" ");
          final int bytes = Integer.parseInt(ss[1]);
          if (bytes == 0)
            return true;
          byte[] buf = readBinary(bytes, readListener);

          if (compressed)
            buf = Zip.decompress(buf);

          result = buf;
          return true;
        } catch (final SocketTimeoutException e) {
          logger.warning("WWSClient.getData() timeout.");
        } catch (final IOException e) {
          logger.warning("WWSClient.getData() IOException: " + e.getMessage());
        } catch (final NumberFormatException e) {
          logger.warning(
              "WWSClent.getData() couldn't parse server response. Is remote server a Winston Wave Server?");
        }
        return false;
      }
    };
    try {
      ret = rt.go();
    } catch (final UtilException e) {
      // Do nothing
    }
    return ret;
  }

  public List<Channel> getChannels() {
    return getChannels(false);
  }

  public List<Channel> getChannels(final boolean meta) {
    String[] result = null;
    final Retriable<String[]> rt = new Retriable<String[]>("WWSClient.getChannels()", maxRetries) {
      @Override
      public void attemptFix() {
        close();
      }

      @Override
      public boolean attempt() throws UtilException {
        try {
          if (!connected())
            connect();

          String cmd = "GETCHANNELS: GC";
          if (meta)
            cmd += " METADATA";
          writeString(cmd + "\n");
          final String info = readString();
          String[] ss = info.split(" ");
          final int lines = Integer.parseInt(ss[1]);
          if (lines == 0)
            return true;
          ss = new String[lines];
          for (int i = 0; i < ss.length; i++)
            ss[i] = readString();

          result = ss;
          return true;
        } catch (final SocketTimeoutException e) {
          logger.warning("WWSClient.getChannels() timeout.");
        } catch (final IOException e) {
          logger.warning("WWSClient.getChannels() IOException: " + e.getMessage());
        }
        return false;
      }
    };
    try {
      result = rt.go();
    } catch (final UtilException e) {
      // Do nothing
    }

    if (result == null)
      return null;

    final List<Channel> chs = new ArrayList<Channel>(result.length);
    for (final String s : result) {
      final Channel ch = new Channel(s);
      chs.add(ch);
    }

    return chs;
  }

  public Wave getWave(final String station, final String comp, final String network,
      final String location, final double start, final double end, final boolean compress) {
    final String req = String.format(Locale.US, "GETWAVERAW: GS %s %s %s %s %f %f %s\n", station,
        comp, network, (location == null ? "--" : location), start, end, (compress ? "1" : "0"));
    final byte[] buf = getData(req, compress);
    if (buf == null)
      return null;

    return new Wave(ByteBuffer.wrap(buf));
  }

  public HelicorderData getHelicorder(final String station, final String comp, final String network,
      final String location, final double start, final double end, final boolean compress) {
    final String req = String.format(Locale.US, "GETSCNLHELIRAW: GS %s %s %s %s %f %f %s\n",
        station, comp, network, location, start, end, (compress ? "1" : "0"));
    final byte[] buf = getData(req, compress);
    if (buf == null)
      return null;

    return new HelicorderData(ByteBuffer.wrap(buf));
  }

  public String[] getStatus() throws UtilException {
    return getStatus(0d);
  }

  public String[] getStatus(final Double d) throws UtilException {
    final double ageThreshold = d;
    final Retriable<String[]> rt = new Retriable<String[]>("WWSClient.getStatus()", maxRetries) {
      @Override
      public void attemptFix() {
        close();
      }

      @Override
      public boolean attempt() {
        try {
          if (!connected())
            connect();

          final String cmd = "STATUS: GC " + ageThreshold;
          writeString(cmd + "\n");

          final String info = readString();
          String[] ss = info.split(": ");
          final int lines = Integer.parseInt(ss[1]);
          if (lines == 0)
            return true;

          ss = new String[lines];
          for (int i = 0; i < ss.length; i++)
            ss[i] = readString();

          result = ss;
          return true;
        } catch (final SocketTimeoutException e) {
          logger.warning("WWSClient.getStatus() timeout.");
        } catch (final IOException e) {
          logger.warning("WWSClient.getChannels() IOException: " + e.getMessage());
        }
        return false;
      }
    };

    return rt.go();
  }

  public RSAMData getRSAMData(final String station, final String comp, final String network,
      final String location, final double start, final double end, final int period,
      final boolean compress) {
    final String req = String.format(Locale.US, "GETSCNLRSAMRAW: GS %s %s %s %s %f %f %d %s\n",
        station, comp, network, location, start, end, period, (compress ? "1" : "0"));
    final byte[] buf = getData(req, compress);
    if (buf == null)
      return null;

    return new RSAMData(ByteBuffer.wrap(buf), period);
  }

  public static void outputSac(final String s, final int p, final Double st, final Double et,
      final String c) {
    final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
    final String date = df.format(J2kSec.asDate(st)) + "-" + df.format(J2kSec.asDate(et));

    outputSac(s, p, st, et, c, c.replace('$', '_') + "_" + date + ".sac");
  }

  public static void outputSac(final String s, final int p, final Double st, final Double et,
      final String c, final String fn) {
    final WWSClient winston = new WWSClient(s, p);
    winston.connect();

    final String[] chan = c.split("\\$");
    final String sta = chan[0];
    final String comp = chan[1];
    final String net = chan[2];
    final String loc = chan.length == 4 ? chan[3] : null;

    Wave wave = winston.getWave(sta, comp, net, loc, st, et, false);

    if (wave != null) {
      wave = wave.subset(st, et);

      final SeismicDataFile file = SeismicDataFile.getFile(fn, FileType.SAC);
      String channel = sta + "_" + comp + "_" + net;
      if (loc != null)
        channel += "_" + loc;

      file.putWave(channel, wave);
      try {
        file.write();
      } catch (final IOException e) {
        System.err.println("Couldn't write file: " + e.getLocalizedMessage());
        e.printStackTrace();
      }
    } else {
      System.out.println("Wave not found");
    }
  }

  public static void outputSac(final String server, final int port, final Double st,
      final Double et, final String c, final String fn, final double gulpSize,
      final double gulpDelay) {
    final WWSClient winston = new WWSClient(server, port);
    winston.connect();

    final String[] chan = c.split("\\$");
    final String sta = chan[0];
    final String comp = chan[1];
    final String net = chan[2];
    final String loc = chan.length == 4 ? chan[3] : null;

    final List<Wave> waves = new ArrayList<Wave>();

    final double duration = et - st;
    final int N = (int) Math.ceil(duration / gulpSize) - 1;
    double t1 = st;
    double t2 = 0;
    Wave wavelet;
    System.out.printf("Gulp size: %f (s), Gulp delay: %d (ms), Number of gulps: %d\n", gulpSize,
        (long) (gulpDelay * 1000), N + 1);
    for (int i = 0; i < N; i++) {
      t2 = t1 + gulpSize;
      System.out.printf("Gulp #%d starting ... ", i + 1);
      wavelet = winston.getWave(sta, comp, net, loc, t1, t2, false);
      System.out.printf("done.\n");
      if (wavelet != null)
        waves.add(wavelet);
      t1 = t2;
      if (gulpDelay != 0)
        try {
          System.out.printf("Waiting ... ");
          Thread.sleep((long) (gulpDelay * 1000));
          System.out.println("done.");

        } catch (final InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

    }
    t2 = et;
    System.out.printf("Gulp #%d starting ... ", N + 1);
    wavelet = winston.getWave(sta, comp, net, loc, t1, t2, false);
    System.out.printf("done.\n");

    if (wavelet != null)
      waves.add(wavelet);

    Wave wave = Wave.join(waves);

    if (wave != null) {
      wave = wave.subset(st, et);

      final SeismicDataFile file = SeismicDataFile.getFile(fn, FileType.SAC);
      String channel = sta + "_" + comp + "_" + net;
      if (loc != null)
        channel += "_" + loc;

      file.putWave(channel, wave);
      try {
        file.write();
      } catch (final IOException e) {
        System.err.println("Couldn't write file: " + e.getLocalizedMessage());
        e.printStackTrace();
      }
    } else {
      System.out.println("Wave not found");
    }
  }

  public static void outputText(final String s, final int p, final Double st, final Double et,
      final String c) {
    System.out.println("dumping samples as text\n");
    final WWSClient winston = new WWSClient(s, p);
    winston.connect();

    final String[] chan = c.split("\\$");
    final String sta = chan[0];
    final String comp = chan[1];
    final String net = chan[2];
    final String loc = chan.length == 4 ? chan[3] : null;

    Wave wave = winston.getWave(sta, comp, net, loc, st, et, false);

    if (wave != null) {
      wave = wave.subset(st, et);
      for (final int i : wave.buffer)
        System.out.println(i);

    } else {
      System.out.println("Wave not found");
    }
  }

  public static void main(final String[] as) {
    final DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));

    Double st = null;
    Double et = null;
    String s;
    int p;
    String c;

    final Set<String> keys = new HashSet<String>();
    final Set<String> flags = new HashSet<String>();

    flags.add("--help");
    flags.add("-sac");
    flags.add("-txt");
    keys.add("-s");
    keys.add("-p");
    keys.add("-st");
    keys.add("-et");
    keys.add("-c");

    final Arguments args = new Arguments(as, flags, keys);

    if (args.flagged("--help") || as.length == 0) {
      System.err.println("java gov.usgs.volcanoes.winston.server.WWSClient [OPTIONS]\n");
      System.out.println("-s [server]\t\tWinston server");
      System.out.println("-p [port]\t\tport");
      System.out.println("-st [yyyymmddHHmmss]\tstart time");
      System.out.println("-et [yyyymmddHHmmss]\tend time");
      System.out.println("-c [s$c$n$l]\t\tchannel");
      System.out.println("-sac\t\t\toutput sac file");
      System.out.println("-txt\t\t\toutput text");
      System.exit(-1);
    }

    s = args.get("-s");
    p = Integer.parseInt(args.get("-p"));

    try {
      st = J2kSec.fromDate(df.parse(args.get("-st")));
      et = J2kSec.fromDate(df.parse(args.get("-et")));
    } catch (final ParseException e) {
      e.printStackTrace();
    }
    c = args.get("-c");

    if (args.flagged("-sac")) {
      System.out.println(s + ":" + p + ":" + st + ":" + et + ":" + c);
      outputSac(s, p, st, et, c);
    }

    if (args.flagged("-txt"))
      outputText(s, p, st, et, c);

  }

}
