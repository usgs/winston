package gov.usgs.volcanoes.winston.server.httpCmd;

import gov.usgs.net.HttpRequest;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public interface HttpCommand {
  abstract public void doCommand(ChannelHandlerContext ctx, FullHttpRequest request) throws MalformedCommandException, UtilException;
  
  /**
   * Command as a http file
   * 
   * @return command, including leading /
   */
  abstract public String getCommand();

  /**
   * Text used as anchor to navigate usage page
   * 
   * @return anchor text
   */
  abstract public String getAnchor();

  /**
   * Command title as displayed on usage page
   * 
   * @return command title
   */
  abstract public String getTitle();

  /**
   * Usage text to be included on usagpage. Embeded HTML is okay.
   * 
   * @return usage text
   */
  abstract public String getUsage(HttpRequest req);
}