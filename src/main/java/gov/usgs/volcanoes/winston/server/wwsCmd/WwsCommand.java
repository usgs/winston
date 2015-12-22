package gov.usgs.volcanoes.winston.server.wwsCmd;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import io.netty.channel.ChannelHandlerContext;

public interface WwsCommand {
  abstract public void doCommand(ChannelHandlerContext ctx, WwsCommandString req) throws MalformedCommandException, UtilException;
}