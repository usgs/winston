package gov.usgs.volcanoes.winston.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

public class TrafficCounter extends ChannelTrafficShapingHandler {

  public TrafficCounter(long checkInterval) {
    super(checkInterval);
  }

  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    System.out.println(":::::Channel unregistered");
  }
}
