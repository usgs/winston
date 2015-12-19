/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;


import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

import gov.usgs.volcanoes.core.Log;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.Version;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;

/**
 * The Winston Wave Server. This program mimics the network protocol of the Earthworm Wave Server.
 *
 * TODO: either get rid of maxDataSize, etc. comments or reimplement them. TODO: minimal UI? TODO:
 * keystrokes status?
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class WWS {
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

    boolean run = true;
    Console console = System.console();
    if (console != null) {
      while (run) {
        System.out.println("Enter ? for console commands.");
        String s = console.readLine();

        if (s == null)
          continue;

        s = s.toLowerCase().trim();
        if (s.equals("q")) {
          run = false;
          wws.shutdownGracefully();
           } else if (s.startsWith("c")) {
           wws.printConnections(s);
          // } else if (s.startsWith("m")) {
          // wws.printCommands(s);
          // } else if (s.equals("d")) {
          // wws.dropConnections(wws.idleTime);
          // } else if (s.startsWith("t")) {
          // wws.toggleTrace(s);
        } else if (s.equals("0")) {
          org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
        } else if (s.equals("1")) {
          org.apache.log4j.Logger.getRootLogger().setLevel(Level.WARN);
        } else if (s.equals("2")) {
          org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
        } else if (s.equals("3")) {
          org.apache.log4j.Logger.getRootLogger().setLevel(Level.ALL);
        } else if (s.equals("?")) {
          WWS.printKeys();
        } else {
          WWS.printKeys();
        }
      }
    } else {
      System.out.println("No console present. Unable to accept console commands.");
    }
  }

  public void printConnections(String s) {
    System.out.println(connectionStatistics);
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

  private ConfigFile configFile;
  protected String configFilename;

  protected int embargo = 0;
  protected int handlers;
  protected int httpMaxSize;
  protected int httpRefreshInterval;
  protected long idleTime;

  private boolean keepalive;
  protected String logFile;

  protected int logNumFiles;

  protected int logSize;

  protected int maxDays = 0;

  private InetAddress serverIp;

  private int serverPort;

  protected int slowCommandTime;

  protected String winstonDriver;

  protected String winstonPrefix;

  protected int winstonStatementCacheCap;

  protected String winstonURL;
  protected NioEventLoopGroup group;
  private boolean acceptCommands;

  private final ConnectionStatistics connectionStatistics;
  /**
   * Creates a new WWS.
   */
  public WWS(final String cf) {
    super();

    connectionStatistics = new ConnectionStatistics();
    acceptCommands = true;
    LOGGER.info(Version.VERSION_STRING);
    configFilename = cf;
    processConfigFile();
  }

  protected void fatalError(final String msg) {
    LOGGER.error(msg);
    System.exit(1);
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
    LOGGER.info("Launching WWS. {}", Version.VERSION_STRING);
    group = new NioEventLoopGroup();

    // TODO: figure out how much flexibility is needed here
    final GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setMinIdle(1);

    final ConfigFile winstonConfig = configFile.getSubConfig("winston");
    final WinstonDatabasePool databasePool = new WinstonDatabasePool(winstonConfig, poolConfig);
//    final GlobalChannelTrafficShapingHandler trafficCounter = new GlobalChannelTrafficShapingHandler(Executors.newScheduledThreadPool(1));

    final AttributeKey<ConnectionStatistics> connectionStatsKey =
        AttributeKey.valueOf("connectionStatistics");
//    final ConnectionStatistics connectionStatistics = new ConnectionStatistics();


//    new Thread() {
//      public void run() {
//        while (true) {
//          System.out.println(connectionStatistics);
//          try {
//            Thread.sleep(2 * 1000);
//          } catch (InterruptedException ignored) {
//            // TODO Auto-generated catch block
//          }
//        }
//      }
//    }.start();

    // final AttributeKey<DatabaseStatistics> databaseStatsKey =
    // AttributeKey.valueOf("databaseStatistics");
    // final DatabaseStatistics databaseStatistics = new DatabaseStatistics();

    final ServerBootstrap b = new ServerBootstrap();
    b.group(group).channel(NioServerSocketChannel.class)
        .localAddress(new InetSocketAddress(serverIp, serverPort))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            connectionStatistics.incrOpenCount();
            final ChannelTrafficShapingHandler trafficCounter = new ChannelTrafficShapingHandler(1000);
            final InetSocketAddress remoteAddress = ch.remoteAddress();
            connectionStatistics.mapChannel(remoteAddress, trafficCounter);
 
            ch.pipeline().addLast(trafficCounter);
            ch.pipeline().addLast(new PortUnificationDecoder(configFile, databasePool));

            ch.attr(connectionStatsKey).set(connectionStatistics);
            // ch.attr(databaseStatsKey).set(databaseStatistics);
            ch.closeFuture().addListener(new ChannelFutureListener() {
              public void operationComplete(ChannelFuture future) throws Exception {
                connectionStatistics.decrOpenCount();
                connectionStatistics.unmapChannel(remoteAddress);

                System.out.println("Total: " + trafficCounter.trafficCounter().cumulativeWrittenBytes());
              }
            });
            
          }
        });

    final ChannelFuture f = b.bind();
    while (!f.isDone())

    {
      try {
        f.sync();
      } catch (final InterruptedException ignore) {
        // do nothing
      }

    }
    if (f.isSuccess())

    {
      LOGGER.info("WWS started and listen on {}", f.channel().localAddress());
    } else

    {
      LOGGER.error("Unable to start server.");
      shutdownGracefully();
    }

  }

  public void shutdownGracefully() {
    LOGGER.warn("shutting down");

    acceptCommands = false;
    final Future<?> ff = group.shutdownGracefully();
    try {
      ff.sync();
    } catch (final InterruptedException ignore) {
      // do nothing
    }
  }

  public int maxDays() {
    return maxDays;
  }

  /**
   * Processes the configuration file (default 'WWS.config'). See the default file for documentation
   * on the different options.
   */
  public void processConfigFile() {
    configFile = new ConfigFile(configFilename);
    if (!configFile.wasSuccessfullyRead()) {
      fatalError(configFilename + ": could not read config file.");
    }

    final String a = configFile.getString("wws.addr");
    if (a != null) {
      try {
        serverIp = InetAddress.getByName(a);
      } catch (final UnknownHostException e) {
        LOGGER.info("unknown host {}", a);
      }
      LOGGER.info("config: wws.addr={}.", serverIp.getCanonicalHostName());
    }

    final int p = StringUtils.stringToInt(configFile.getString("wws.port"), -1);
    if (p < 0 || p > 65535) {
      fatalError(configFilename + ": bad or missing 'wws.port' setting.");
    }
    serverPort = p;
    LOGGER.info("config: wws.port={}.", serverPort);

    final boolean k = StringUtils.stringToBoolean(configFile.getString("wws.keepalive"), false);
    keepalive = k;
    LOGGER.info("config: wws.keepalive={}.", keepalive);

    // final int h = StringUtils.stringToInt(cf.getString("wws.handlers"), -1);
    // if (h < 1 || h > 128) {
    // fatalError(configFilename + ": bad or missing 'wws.handlers' setting.");
    // }
    // handlers = h;
    // LOGGER.info("config: wws.handlers={}.", handlers);

    // final int m = StringUtils.stringToInt(cf.getString("wws.maxConnections"), -1);
    // if (m < 0) {
    // fatalError(configFilename + ": bad or missing 'wws.maxConnections' setting.");
    // }
    //
    // connections.setMaxConnections(m);
    // logger.info("config: wws.maxConnections=" + connections.getMaxConnections() + ".");

    // final long i = 1000 * StringUtils.stringToInt(cf.getString("wws.idleTime"), 7200);
    // if (i < 0) {
    // fatalError(configFilename + ": bad or missing 'wws.idleTime' setting.");
    // }
    //
    // idleTime = i;
    // logger.info("config: wws.idleTime=" + idleTime / 1000 + ".");
    //
    // embargo = 0; // TODO: implement

    maxDays = StringUtils.stringToInt(configFile.getString("wws.maxDays"), 0);
    LOGGER.info("config: wws.maxDays={}.", maxDays);

    if (configFile.getString("wws.allowHttp") == null) {
      fatalError(configFilename + ": missing 'wws.allowHttp' setting.");
    } else {
      allowHttp = StringUtils.stringToBoolean(configFile.getString("wws.allowHttp"));
    }
    LOGGER.info("config: wws.allowHttp=" + allowHttp + ".");

    httpMaxSize = StringUtils.stringToInt(configFile.getString("wws.httpMaxSize"), 10000000);
    LOGGER.info("config: wws.httpMaxSize=" + httpMaxSize + ".");

    slowCommandTime = StringUtils.stringToInt(configFile.getString("wws.slowCommandTime"), 1500);
    LOGGER.info("config: wws.slowCommandTime=" + slowCommandTime + ".");

    httpRefreshInterval =
        StringUtils.stringToInt(configFile.getString("wws.httpRefreshInterval"), 0);
    LOGGER.info("config: wws.httpRefreshInterval=" + httpRefreshInterval + ".");

    winstonDriver = configFile.getString("winston.driver");
    winstonURL = configFile.getString("winston.url");
    winstonPrefix = configFile.getString("winston.prefix");
    winstonStatementCacheCap =
        StringUtils.stringToInt(configFile.getString("winston.statementCacheCap"), 100);
  }
}
