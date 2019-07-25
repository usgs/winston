/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;


import java.io.Console;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.Log;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;

/**
 * The Winston Wave Server. This program mimics the network protocol of the Earthworm Wave Server.
 *
 * TODO: either get rid of maxDataSize, etc. comments or reimplement them.
 * TODO: minimal UI?
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class WWS {
  private static final Logger LOGGER = LoggerFactory.getLogger(WWS.class);
  private static final int DEFAULT_DB_CONNECTIONS = 5;

  /**
   * Launch the WWS.
   *
   * @param args command line arguments
   * @throws Exception when command line cannot be parsed
   */
  public static void main(final String[] args) throws Exception {

    final WWSArgs config = new WWSArgs(args);
    Log.addFileAppender(new File(config.logDir, "WWS.log").toString());
    final WWS wws = new WWS(config.configFileName);

    wws.launch();
    
    if (config.isVerbose) {
      org.apache.log4j.Logger.getRootLogger().setLevel(Level.ALL);
      System.out.println("Logging level set to \"All\"");
    } else {
      org.apache.log4j.Logger.getRootLogger().setLevel(Level.WARN);
      System.out.println("Logging level set to \"Warn\"");
    }
    
    if (System.console() == null) {
      System.out.println("No console present. I will not listen for console commands.");
    } else {
      attendConsole(wws);
    }
  }

  private static void attendConsole(WWS wws) {
    boolean run = true;
    Console console = System.console();

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
        System.out.println(wws.connectionStatistics.printConnections(s));
        // } else if (s.startsWith("m")) {
        // wws.printCommands(s);
        // } else if (s.equals("d")) {
        // wws.dropConnections(wws.idleTime);
        // } else if (s.startsWith("t")) {
        // wws.toggleTrace(s);
      } else if (s.equals("0")) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);
        System.out.println("Logging level set to \"Error\"");
      } else if (s.equals("1")) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.WARN);
        System.out.println("Logging level set to \"Warn\"");
      } else if (s.equals("2")) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
        System.out.println("Logging level set to \"Info\"");
      } else if (s.equals("3")) {
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ALL);
        System.out.println("Logging level set to \"All\"");
      } else {
        WWS.printKeys();
      }
    }
    System.exit(0);
  }

  private static void printKeys() {
    final StringBuffer sb = new StringBuffer();
    sb.append(Version.VERSION_STRING + "\n");
    sb.append("Keys:\n");
    sb.append("        ?: display keys\n");
    sb.append("      0-3: logging level. Larger number for more logging.\n");
    sb.append("        q: quit after servicing open connections.\n");
    sb.append("        c: print connections sorted by bytes transmited\n");
    sb.append("       cA: print connections sorted by address\n");
    sb.append("       cC: print connections sorted by connect time\n");
    sb.append("       cL: print connections sorted by last request time\n");
    sb.append("       cR: print connections sorted by bytes received\n");
    sb.append("       cT: print connections sorted by bytes transmited\n");
    sb.append("         : append '-' to sort in descending order.\n");

    System.out.println(sb);
  }

  private final ConfigFile configFile;
  protected final int dbConnections;
  private final InetAddress serverIp;
  private final int serverPort;
  private NioEventLoopGroup group;
  private final ConnectionStatistics connectionStatistics;

  /**
   * Constructor.
   * @param cf config file
   * @throws UtilException when things go wrong
   */
  public WWS(final String cf) throws UtilException {
    connectionStatistics = new ConnectionStatistics();
    LOGGER.info(Version.VERSION_STRING);
    String configFilename = cf;

    configFile = new ConfigFile(configFilename);
    if (!configFile.wasSuccessfullyRead()) {
      throw new RuntimeException(String.format("%s: could not read config file.", configFilename));
    }

    final String addr = configFile.getString("wws.addr");
    if (addr != null) {
      try {
        serverIp = InetAddress.getByName(addr);
      } catch (final UnknownHostException e) {
        throw new UtilException(
            String.format("%s: unable to resolve configured wws.addr. (%s)", configFilename, addr));
      }
      LOGGER.info("config: wws.addr={}.", serverIp.getCanonicalHostName());
    } else {
      serverIp = null;
    }

    final int port = StringUtils.stringToInt(configFile.getString("wws.port"), -1);
    if (port < 0 || port > 65535) {
      throw new UtilException(
          String.format("%s: bad or missing 'wws.port' setting. (%s)", configFilename, port));
    } else {
      serverPort = port;
      LOGGER.info("config: wws.port={}.", serverPort);
    }

    dbConnections =
        StringUtils.stringToInt(configFile.getString("wws.dbConnections"), DEFAULT_DB_CONNECTIONS);
    LOGGER.info("config: wws.dbConnections={}.", dbConnections);
  }

  /**
   * Start listening.
   */
  public void launch() {
    LOGGER.info("Launching WWS. {}", Version.VERSION_STRING);
    group = new NioEventLoopGroup();

    // TODO: figure out how much flexibility is needed here
    final GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setMaxTotal(dbConnections);
    final ConfigFile winstonConfig = configFile.getSubConfig("winston");
    long maxDays = configFile.getLong("wws.maxDays", 0);
    if (maxDays == 0)
      maxDays = WinstonDatabase.MAX_DAYS_UNLIMITED;
    
    winstonConfig.put("maxDays", "" + maxDays);
    final WinstonDatabasePool databasePool = new WinstonDatabasePool(winstonConfig, poolConfig);

    final AttributeKey<ConnectionStatistics> connectionStatsKey =
        AttributeKey.valueOf("connectionStatistics");

    final ServerBootstrap b = new ServerBootstrap();
    b.group(group).channel(NioServerSocketChannel.class)
        .localAddress(new InetSocketAddress(serverIp, serverPort))
        .option(ChannelOption.SO_KEEPALIVE, true)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            connectionStatistics.incrOpenCount();
            final ChannelTrafficShapingHandler trafficCounter =
                new ChannelTrafficShapingHandler(0);
            final InetSocketAddress remoteAddress = ch.remoteAddress();
            connectionStatistics.mapChannel(remoteAddress, trafficCounter);

            ch.pipeline().addLast(trafficCounter);
            ch.pipeline().addLast(new PortUnificationDecoder(configFile, databasePool));

            ch.attr(connectionStatsKey).set(connectionStatistics);
            ch.closeFuture().addListener(new ChannelFutureListener() {
              public void operationComplete(ChannelFuture future) throws Exception {
                connectionStatistics.decrOpenCount();
                connectionStatistics.unmapChannel(remoteAddress);
              }
            });
          }
        });

    final ChannelFuture f = b.bind();
    while (!f.isDone()) {
      try {
        f.sync();
      } catch (final InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
      }
    }
    if (f.isSuccess()) {
      LOGGER.info("WWS started and listen on {}", f.channel().localAddress());
    } else {
      LOGGER.error("Unable to start server.");
      shutdownGracefully();
    }
  }

  private void shutdownGracefully() {
    LOGGER.warn("shutting down");

    final Future<?> ff = group.shutdownGracefully();
    try {
      ff.sync();
    } catch (final InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(ex);
    }
  }

}
