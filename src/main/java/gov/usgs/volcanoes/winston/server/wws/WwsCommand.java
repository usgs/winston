/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import io.netty.channel.ChannelHandlerContext;

/**
 * A winston command
 * @author Tom Parker
 *
 */
public interface WwsCommand {
  /**
   * Execute the command
   * @param ctx my context
   * @param req the request
   * @throws MalformedCommandException when I cannot understand the request
   * @throws UtilException when things go wrong
   */
  abstract public void doCommand(ChannelHandlerContext ctx, WwsCommandString req) throws MalformedCommandException, UtilException;
}