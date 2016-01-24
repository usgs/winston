/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * Just a place holder. To be removed soon.
 * 
 * @author Tom Parker
 *
 */
@Deprecated
public class TrafficCounter extends ChannelTrafficShapingHandler {

  public TrafficCounter(long checkInterval) {
    super(checkInterval);
  }

  public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
    System.out.println(":::::Channel unregistered");
  }
}
