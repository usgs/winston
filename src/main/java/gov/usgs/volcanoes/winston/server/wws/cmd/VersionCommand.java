/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return the server menu
 * 
 * <cdm> = "VERSION"
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class VersionCommand extends WwsBaseCommand {
  /** Winston protocol version   */
  public static final int PROTOCOL_VERSION = 3;

  /**
   * Constructor.
   */
  public VersionCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException {

    ctx.write("PROTOCOL_VERSION: " + PROTOCOL_VERSION + "\n");
  }
}
