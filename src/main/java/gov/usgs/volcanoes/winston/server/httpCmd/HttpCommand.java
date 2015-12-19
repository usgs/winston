package gov.usgs.volcanoes.winston.server.httpCmd;

import gov.usgs.volcanoes.winston.server.wwsCmd.MalformedCommandException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface HttpCommand {
  abstract public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request) throws MalformedCommandException;
}