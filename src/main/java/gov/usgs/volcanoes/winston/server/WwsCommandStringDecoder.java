package gov.usgs.volcanoes.winston.server;

import java.util.List;

import gov.usgs.volcanoes.winston.server.wwsCmd.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class WwsCommandStringDecoder extends MessageToMessageDecoder<String> {

    @Override
    protected void decode(ChannelHandlerContext ctx, String command, List<Object> out) {
        out.add(new WwsCommandString(command));
    }
}