package gov.usgs.volcanoes.winston.server.wwsCmd;

import io.netty.channel.ChannelHandlerContext;

public interface WwsCommand {
  abstract public void doCommand(ChannelHandlerContext ctx, WwsCommandString req) throws MalformedCommandException;
}