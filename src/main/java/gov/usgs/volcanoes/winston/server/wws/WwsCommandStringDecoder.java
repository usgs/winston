/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * WWS command decoder.
 * 
 * @author Tom Parker
 *
 */
public class WwsCommandStringDecoder extends MessageToMessageDecoder<String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WwsCommandStringDecoder.class);
  
  @Override
  protected void decode(ChannelHandlerContext ctx, String command, List<Object> out) {
    try {
      out.add(new WwsCommandString(command));
    } catch (MalformedCommandException ex) {
      LOGGER.info("Bad request: {}", ex);
    }
  }
}
