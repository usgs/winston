/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;


import java.io.Console;
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

    Log.addFileAppender("WWS.log");

    final WWSArgs config = new WWSArgs(args);
    if (config.isVerbose) {
      org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
    }

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
        } else if (s.equals("?")) {
          WWS.printKeys();
        } else {
          WWS.printKeys();
        }
      }
    } else {
      System.out.println("No console present. I will not listen for console commands.");
    }
  }

  public void printConnections(String s) {
    System.out.println(connectionStatistics.printConnections(s));
  }

  public static void printKeys() {
    final StringBuffer sb = new StringBuffer();
    sb.append(Version.VERSION_STRING + "\n");
    sb.append("Keys:\n");
    sb.append(" 0-3: logging level\n");
    // sb.append(" d: drop idle connections\n");
    sb.append("        q: quit\n");
    sb.append("        ?: display keys\n");
    // sb.append(" m: print running commands\n");
    // sb.append(" t<index>: toggle tracing of a connection\n");
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

  private ConfigFile configFile;
  protected String configFilename;
  protected int dbConnections;
  private InetAddress serverIp;
  private int serverPort;
  private NioEventLoopGroup group;
  private final ConnectionStatistics connectionStatistics;

  /**
   * Creates a new WWS.
   */
  public WWS(final String cf) throws UtilException {
    super();

    connectionStatistics = new ConnectionStatistics();
    LOGGER.info(Version.VERSION_STRING);
    configFilename = cf;
    processConfigFile();
  }

  protected void fatalError(final String msg) {
    LOGGER.error(msg);
    System.exit(1);
  }

  public void launch() {
    LOGGER.info("Launching WWS. {}", Version.VERSION_STRING);
    group = new NioEventLoopGroup();

    // TODO: figure out how much flexibility is needed here
    final GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setMaxTotal(dbConnections);

    final ConfigFile winstonConfig = configFile.getSubConfig("winston");
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
                new ChannelTrafficShapingHandler(3000);
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

    final Future<?> ff = group.shutdownGracefully();
    try {
      ff.sync();
    } catch (final InterruptedException ignore) {
      // do nothing
    }
  }

  /**
   * Processes the configuration file (default 'WWS.config'). See the default file for documentation
   * on the different options.
   */
  public void processConfigFile() throws UtilException {
    configFile = new ConfigFile(configFilename);
    if (!configFile.wasSuccessfullyRead()) {
      fatalError(configFilename + ": could not read config file.");
    }

    final String a = configFile.getString("wws.addr");
    if (a != null) {
      try {
        serverIp = InetAddress.getByName(a);
      } catch (final UnknownHostException e) {
        throw new UtilException(configFilename + ": unable to resolve configured wws.addr.");
      }
      LOGGER.info("config: wws.addr={}.", serverIp.getCanonicalHostName());
    }

    final int p = StringUtils.stringToInt(configFile.getString("wws.port"), -1);
    if (p < 0 || p > 65535) {
      throw new UtilException(configFilename + ": bad or missing 'wws.port' setting.");
    }
    serverPort = p;
    LOGGER.info("config: wws.port={}.", serverPort);

    dbConnections =
        StringUtils.stringToInt(configFile.getString("wws.dbConnections"), DEFAULT_DB_CONNECTIONS);
    LOGGER.info("config: wws.dbConnections={}.", dbConnections);
  }
}
