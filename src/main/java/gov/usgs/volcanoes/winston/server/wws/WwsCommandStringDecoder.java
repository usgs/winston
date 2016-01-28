/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * WWS command decoder.
 * 
 * @author Tom Parker
 *
 */
public class WwsCommandStringDecoder extends MessageToMessageDecoder<String> {

  @Override
  protected void decode(ChannelHandlerContext ctx, String command, List<Object> out) {
    out.add(new WwsCommandString(command));
  }
}
