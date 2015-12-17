/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpBaseCommand;
import gov.usgs.volcanoes.winston.server.httpCmd.HttpCommand;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * Derived from HttpSnoopServerHandler
 *
 * @author Tom Parker
 *
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerHandler.class);

  private final WinstonDatabasePool winstonDatabasePool;

  public HttpServerHandler(ConfigFile configFile, WinstonDatabasePool winstonDatabasePool) {
    this.winstonDatabasePool = winstonDatabasePool;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {

    final HttpBaseCommand httpWorker = HttpCommand.get(winstonDatabasePool, req.getUri());
    httpWorker.doCommand(ctx, req);

    // If keep-alive is not set, close the connection once the content is fully written.
    if (!HttpHeaders.isKeepAlive(req)) {
      ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
