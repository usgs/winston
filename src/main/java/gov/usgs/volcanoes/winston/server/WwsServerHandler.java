package gov.usgs.volcanoes.winston.server;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.server.wwsCmd.UnsupportedCommandException;
import gov.usgs.volcanoes.winston.server.wwsCmd.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wwsCmd.WwsCommand;
import gov.usgs.volcanoes.winston.server.wwsCmd.WwsCommandString;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author <a href="mailto:norman.maurer@googlemail.com">Norman Maurer</a>
 */
public class WwsServerHandler extends SimpleChannelInboundHandler<WwsCommandString> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WwsServerHandler.class);
  private static final int DEFAULT_MIN_IDLE = 2;
  
  private ConfigFile configFile;
  private WinstonDatabasePool winstonDatabasePool;
  
  public WwsServerHandler(ConfigFile configFile) {
    this.configFile = configFile;
    ConfigFile winstonConfig = configFile.getSubConfig("winston");
 
    // TODO: figure out how much flexability is needed here
    GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
    poolConfig.setMinIdle(DEFAULT_MIN_IDLE);
    
    winstonDatabasePool = new WinstonDatabasePool(winstonConfig, poolConfig);
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, WwsCommandString request) throws Exception {
    
    try {
    final WwsBaseCommand wwsWorker = WwsCommand.get(winstonDatabasePool, request);
    wwsWorker.doCommand(ctx, request);
    // If keep-alive is off, close the connection once the content is fully written.
    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    } catch (UnsupportedCommandException e) {
      LOGGER.info(e.getLocalizedMessage());
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }
}
