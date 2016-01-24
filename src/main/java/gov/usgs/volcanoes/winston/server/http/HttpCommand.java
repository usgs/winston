package gov.usgs.volcanoes.winston.server.http;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * A HTTP command.
 * 
 * @author Tom Parker
 *
 */
public interface HttpCommand {
  /**
   * Fulfill a command.
   * 
   * @param ctx my context
   * @param request the request
   * @throws MalformedCommandException when I don't understand the request
   * @throws UtilException when things go wrong
   */
  abstract public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request) throws MalformedCommandException, UtilException;
}