package gov.usgs.volcanoes.winston.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

import gov.usgs.net.Server;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.winston.Version;

/**
 * The Winston Wave Server. This program mimics the network protocol of the
 * Earthworm Wave Server.
 *
 * TODO: either get rid of maxDataSize, etc. comments or reimplement them.
 * TODO: minimal UI?
 * TODO: keystrokes status?
 *
 * @author Dan Cervelli
 */
public class WWS extends Server {
  /**
   * Launch the WWS.
   *
   * @param args
   *          command line arguments
   * @throws Exception when command line cannot be parsed
   */
  public static void main(final String[] args) throws Exception {
    final WWSArgs config = new WWSArgs(args);
    final WWS wws = new WWS(config.configFileName);

    if (config.isVerbose) {
      wws.setLogLevel(Level.ALL);
    } else {
      wws.setLogLevel(Level.FINE);
    }

    wws.launch();

    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    boolean acceptCommands = !(config.isNoInput);
    if (acceptCommands) {
      wws.logger.info("Enter ? for console commands.");
    }

    while (acceptCommands) {
      String s = in.readLine();
      if (s != null) {
        s = s.toLowerCase().trim();
        if (s.equals("q")) {
          acceptCommands = false;
          System.exit(0);
        } else if (s.startsWith("c")) {
          wws.printConnections(s);
        } else if (s.startsWith("m")) {
          wws.printCommands(s);
        } else if (s.equals("d")) {
          wws.dropConnections(wws.idleTime);
        } else if (s.startsWith("t")) {
          wws.toggleTrace(s);
        } else if (s.equals("0")) {
          wws.setLogLevel(Level.SEVERE);
        } else if (s.equals("1")) {
          wws.setLogLevel(Level.FINE);
        } else if (s.equals("2")) {
          wws.setLogLevel(Level.FINER);
        } else if (s.equals("3")) {
          wws.setLogLevel(Level.ALL);
        } else if (s.equals("?")) {
          WWS.printKeys();
        } else {
          WWS.printKeys();
        }
      }
    }
  }

  public static void printKeys() {
    final StringBuffer sb = new StringBuffer();
    sb.append(Version.VERSION_STRING + "\n");
    sb.append("Keys:\n");
    sb.append(" 0-3: logging level\n");
    sb.append("        d: drop idle connections\n");
    sb.append("        q: quit\n");
    sb.append("        ?: display keys\n");
    sb.append("        m: print running commands\n");
    sb.append(" t<index>: toggle tracing of a connection\n");
    sb.append("        c: print connections sorted by bytes transmited\n");
    sb.append("       cA: print connections sorted by address\n");
    sb.append("       cC: print connections sorted by connect time\n");
    sb.append("       cL: print connections sorted by last request time\n");
    sb.append("       cR: print connections sorted by bytes received\n");
    sb.append("       cT: print connections sorted by bytes transmited\n");
    sb.append("       cI: print connections sorted by index\n");
    sb.append("         : append '-' to sort in descending order.\n");

    System.out.println(sb);
  }

  protected boolean allowHttp;

  protected String configFilename;
  protected int embargo = 0;

  protected int handlers;
  protected int httpMaxSize;
  protected int httpRefreshInterval;
  protected long idleTime;
  protected String logFile;

  protected int logNumFiles;
  protected int logSize;

  protected int maxDays = 0;

  protected int slowCommandTime;

  protected String winstonDriver;

  protected String winstonPrefix;

  protected int winstonStatementCacheCap;

  protected String winstonURL;

  /**
   * Creates a new WWS.
   */
  public WWS(final String cf) {
    super();
    name = "WWS";
    logger = Log.getLogger("gov.usgs.winston");

    logger.info(Version.VERSION_STRING);
    configFilename = cf;
    processConfigFile();
    for (int i = 0; i < handlers; i++) {
      addCommandHandler(new ServerHandler(this));
    }
  }

  protected void fatalError(final String msg) {
    logger.severe(msg);
    System.exit(1);
  }

  public int getEmbargo() {
    return embargo;
  }

  public int getMaxDays() {
    return maxDays;
  }

  public int getSlowCommandTime() {
    return slowCommandTime;
  }

  /**
   * Get the Winston database driver.
   *
   * @return the Winston database driver.
   */
  public String getWinstonDriver() {
    return winstonDriver;
  }

  public String getWinstonPrefix() {
    return winstonPrefix;
  }

  public int getWinstonStatementCacheCap() {
    return winstonStatementCacheCap;
  }

  /**
   * Gets the Winston database JDBC URL.
   *
   * @return the Winston database URL
   */
  public String getWinstonURL() {
    return winstonURL;
  }

  public int httpMaxSize() {
    return httpMaxSize;
  }

  public int httpRefreshInterval() {
    return httpRefreshInterval;
  }

  public boolean isHttpAllowed() {
    return allowHttp;
  }

  public void launch() {
    final Thread launchThread = new Thread(new Runnable() {
      public void run() {
        startListening();
      }
    });
    launchThread.start();
  }

  public int maxDays() {
    return maxDays;
  }

  /**
   * Processes the configuration file (default 'WWS.config'). See the default
   * file for documentation on the different options.
   */
  public void processConfigFile() {
    final ConfigFile cf = new ConfigFile(configFilename);
    if (!cf.wasSuccessfullyRead()) {
      fatalError(configFilename + ": could not read config file.");
    }

    final String a = cf.getString("wws.addr");
    if (a != null) {
      try {
        serverIP = InetAddress.getByName(a);
      } catch (final UnknownHostException e) {
        logger.info("unknown host " + a);
      }
      logger.info("config: wws.addr=" + serverIP.getCanonicalHostName() + ".");
    }

    final int p = Util.stringToInt(cf.getString("wws.port"), -1);
    if (p < 0 || p > 65535) {
      fatalError(configFilename + ": bad or missing 'wws.port' setting.");
    }
    serverPort = p;
    logger.info("config: wws.port=" + serverPort + ".");

    final boolean k = Util.stringToBoolean(cf.getString("wws.keepalive"), false);
    keepalive = k;
    logger.info("config: wws.keepalive=" + keepalive + ".");

    final int h = Util.stringToInt(cf.getString("wws.handlers"), -1);
    if (h < 1 || h > 128) {
      fatalError(configFilename + ": bad or missing 'wws.handlers' setting.");
    }
    handlers = h;
    logger.info("config: wws.handlers=" + handlers + ".");

    final int m = Util.stringToInt(cf.getString("wws.maxConnections"), -1);
    if (m < 0) {
      fatalError(configFilename + ": bad or missing 'wws.maxConnections' setting.");
    }

    connections.setMaxConnections(m);
    logger.info("config: wws.maxConnections=" + connections.getMaxConnections() + ".");

    final long i = 1000 * Util.stringToInt(cf.getString("wws.idleTime"), 7200);
    if (i < 0) {
      fatalError(configFilename + ": bad or missing 'wws.idleTime' setting.");
    }

    idleTime = i;
    logger.info("config: wws.idleTime=" + idleTime / 1000 + ".");

    embargo = 0; // TODO: implement

    maxDays = Util.stringToInt(cf.getString("wws.maxDays"), 0);
    logger.info("config: wws.maxDays=" + maxDays + ".");

    if (cf.getString("wws.allowHttp") == null) {
      fatalError(configFilename + ": missing 'wws.allowHttp' setting.");
    } else {
      allowHttp = Util.stringToBoolean(cf.getString("wws.allowHttp"));
    }
    logger.info("config: wws.allowHttp=" + allowHttp + ".");

    httpMaxSize = Util.stringToInt(cf.getString("wws.httpMaxSize"), 10000000);
    logger.info("config: wws.httpMaxSize=" + httpMaxSize + ".");

    slowCommandTime = Util.stringToInt(cf.getString("wws.slowCommandTime"), 1500);
    logger.info("config: wws.slowCommandTime=" + slowCommandTime + ".");

    httpRefreshInterval = Util.stringToInt(cf.getString("wws.httpRefreshInterval"), 0);
    logger.info("config: wws.httpRefreshInterval=" + httpRefreshInterval + ".");

    winstonDriver = cf.getString("winston.driver");
    winstonURL = cf.getString("winston.url");
    winstonPrefix = cf.getString("winston.prefix");
    winstonStatementCacheCap = Util.stringToInt(cf.getString("winston.statementCacheCap"), 100);
  }

  public void setLogLevel(final Level level) {
    // change root logger
    Log.getLogger("gov.usgs").setLevel(level);
    logger.severe("Logging set to " + level);
  }
}
