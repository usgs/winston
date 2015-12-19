/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.server.wwsCmd.UnsupportedCommandException;
import gov.usgs.volcanoes.winston.server.wwsCmd.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wwsCmd.WwsCommandFactory;
import gov.usgs.volcanoes.winston.server.wwsCmd.WwsCommandString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

public class WwsCommandHandler extends SimpleChannelInboundHandler<WwsCommandString> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WwsCommandHandler.class);
  private static final int DEFAULT_MAX_DAYS = 20000;

  private final WinstonDatabasePool winstonDatabasePool;
  private final ConfigFile configFile;
  private ConnectionStatistics connectionStatistics;
  
  private static final AttributeKey<ConnectionStatistics> connectionStatsKey;

  static {
    connectionStatsKey = AttributeKey.valueOf("connectionStatistics");
  }

  public WwsCommandHandler(ConfigFile configFile, WinstonDatabasePool winstonDatabasePool) {
    this.winstonDatabasePool = winstonDatabasePool;
    this.configFile = configFile;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, WwsCommandString request) throws Exception {
    connectionStatistics = ctx.channel().attr(connectionStatsKey).get();

    try {
      final WwsBaseCommand wwsWorker = WwsCommandFactory.get(winstonDatabasePool, request);
      connectionStatistics.incrWwsCount(ctx.channel().remoteAddress());
      int maxDays = configFile.getInt("wws.maxDays", DEFAULT_MAX_DAYS);
      if (maxDays == 0) {
        maxDays = DEFAULT_MAX_DAYS;
      }
      wwsWorker.setMaxDays(maxDays);
      wwsWorker.respond(ctx, request);
    } catch (final UnsupportedCommandException e) {
      LOGGER.info(e.getLocalizedMessage());
      ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    LOGGER.error("Exception caught in WwsServerHandler.", cause);
    ctx.close();
  }
}
