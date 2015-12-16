package gov.usgs.volcanoes.winston.legacyServer;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.net.Server;
import gov.usgs.volcanoes.core.Log;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.server.PortUnificationDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;

/**
 * The Winston Wave Server. This program mimics the network protocol of the Earthworm Wave Server.
 *
 * TODO: either get rid of maxDataSize, etc. comments or reimplement them. TODO: minimal UI? TODO:
 * keystrokes status?
 *
 * @author Dan Cervelli
 */
public class WWS extends Server {
  private static final Logger LOGGER = LoggerFactory.getLogger(WWS.class);

  /**
   * Launch the WWS.
   *
   * @param args command line arguments
   * @throws Exception when command line cannot be parsed
   */
  public static void main(final String[] args) throws Exception {

    Log.addFileAppender("WWS.log");

    final WWSArgs config = new WWSArgs(args);
    final WWS wws = new WWS(config.configFileName);

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
          Log.setLevel(Level.ERROR);
        } else if (s.equals("1")) {
          Log.setLevel(Level.WARN);
        } else if (s.equals("2")) {
          Log.setLevel(Level.INFO);
        } else if (s.equals("3")) {
          Log.setLevel(Level.DEBUG);
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

  private ConfigFile cf;

  /**
   * Creates a new WWS.
   */
  public WWS(final String cf) {
    super();
    name = "WWS";

    LOGGER.info(Version.VERSION_STRING);
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
//    LOGGER.info("Launching WWS. {}", Version.VERSION_STRING);
//    NioEventLoopGroup group = new NioEventLoopGroup();
//    try {
//      // final ChannelHandler handler = new WWSInitializer(cf);
//
//      ServerBootstrap b = new ServerBootstrap();
//      b.group(group).channel(NioServerSocketChannel.class)
//          .localAddress(new InetSocketAddress(serverIP, serverPort))
//          .childHandler(new ChannelInitializer<SocketChannel>() {
//            @Override
//            public void initChannel(SocketChannel ch) throws Exception {
//              ch.pipeline().addLast(new PortUnificationDecoder(cf));
//            }
//          });
//      ChannelFuture f = b.bind();
//      while (!f.isDone()) {
//        try {
//          f.sync();
//        } catch (InterruptedException ignore) {
//          // do nothing
//        }
//      }
//      LOGGER.info("WWS started and listen on {}", f.channel().localAddress());
//
//      ChannelFuture closeF = f.channel().closeFuture();
//      while (!closeF.isDone()) {
//        try {
//          closeF.sync();
//        } catch (InterruptedException ignore) {
//          // do nothing
//        }
//      }
//    } finally {
//      Future<?> ff = group.shutdownGracefully();
//      try {
//        ff.sync();
//      } catch (InterruptedException ignore) {
//        // do nothing
//      }
//    }

  }

  public int maxDays() {
    return maxDays;
  }

  /**
   * Processes the configuration file (default 'WWS.config'). See the default file for documentation
   * on the different options.
   */
  public void processConfigFile() {
    cf = new ConfigFile(configFilename);
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

    final int p = StringUtils.stringToInt(cf.getString("wws.port"), -1);
    if (p < 0 || p > 65535) {
      fatalError(configFilename + ": bad or missing 'wws.port' setting.");
    }
    serverPort = p;
    logger.info("config: wws.port=" + serverPort + ".");

    final boolean k = StringUtils.stringToBoolean(cf.getString("wws.keepalive"), false);
    keepalive = k;
    logger.info("config: wws.keepalive=" + keepalive + ".");

    final int h = StringUtils.stringToInt(cf.getString("wws.handlers"), -1);
    if (h < 1 || h > 128) {
      fatalError(configFilename + ": bad or missing 'wws.handlers' setting.");
    }
    handlers = h;
    logger.info("config: wws.handlers=" + handlers + ".");

    final int m = StringUtils.stringToInt(cf.getString("wws.maxConnections"), -1);
    if (m < 0) {
      fatalError(configFilename + ": bad or missing 'wws.maxConnections' setting.");
    }

    connections.setMaxConnections(m);
    logger.info("config: wws.maxConnections=" + connections.getMaxConnections() + ".");

    final long i = 1000 * StringUtils.stringToInt(cf.getString("wws.idleTime"), 7200);
    if (i < 0) {
      fatalError(configFilename + ": bad or missing 'wws.idleTime' setting.");
    }

    idleTime = i;
    logger.info("config: wws.idleTime=" + idleTime / 1000 + ".");

    embargo = 0; // TODO: implement

    maxDays = StringUtils.stringToInt(cf.getString("wws.maxDays"), 0);
    logger.info("config: wws.maxDays=" + maxDays + ".");

    if (cf.getString("wws.allowHttp") == null) {
      fatalError(configFilename + ": missing 'wws.allowHttp' setting.");
    } else {
      allowHttp = StringUtils.stringToBoolean(cf.getString("wws.allowHttp"));
    }
    logger.info("config: wws.allowHttp=" + allowHttp + ".");

    httpMaxSize = StringUtils.stringToInt(cf.getString("wws.httpMaxSize"), 10000000);
    logger.info("config: wws.httpMaxSize=" + httpMaxSize + ".");

    slowCommandTime = StringUtils.stringToInt(cf.getString("wws.slowCommandTime"), 1500);
    logger.info("config: wws.slowCommandTime=" + slowCommandTime + ".");

    httpRefreshInterval = StringUtils.stringToInt(cf.getString("wws.httpRefreshInterval"), 0);
    logger.info("config: wws.httpRefreshInterval=" + httpRefreshInterval + ".");

    winstonDriver = cf.getString("winston.driver");
    winstonURL = cf.getString("winston.url");
    winstonPrefix = cf.getString("winston.prefix");
    winstonStatementCacheCap =
        StringUtils.stringToInt(cf.getString("winston.statementCacheCap"), 100);
  }
}
