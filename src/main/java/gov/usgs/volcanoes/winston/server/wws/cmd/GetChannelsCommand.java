/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * cmd = "GETCHANNELS" <sp> <id> [ <sp> "METADATA" ]
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetChannelsCommand extends WwsBaseCommand {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(GetChannelsCommand.class);

  /**
   * Constructor.
   */
  public GetChannelsCommand() {
    super();
  }


  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    final boolean metadata = wantsMetadata(cmd);

    List<Channel> chs = null;
    try {
      chs = databasePool.doCommand(new WinstonConsumer<List<Channel>>() {
        public List<Channel> execute(WinstonDatabase winston) {
          Channels channels = new Channels(winston);
          return channels.getChannels(metadata);
        }
      });
    } catch (Exception e) {
      throw new UtilException("Unable to get channels.");
    }

    final StringBuilder sb = new StringBuilder(chs.size() * 60);
    sb.append(String.format("%s %d\n", cmd.id, chs.size()));
    for (final Channel ch : chs) {
      if (metadata)
        sb.append(ch.toMetadataString() + "\n");
      else
        sb.append(ch.toPV2String() + "\n");
    }
    ctx.writeAndFlush(sb.toString());
  }


  private static boolean wantsMetadata(WwsCommandString cmd) throws MalformedCommandException {
    boolean metadata;
    if (cmd.args != null) {
      if (cmd.args.length == 1 && "METADATA".equals(cmd.getString(0))) {
        metadata = true;
      } else {
        throw new MalformedCommandException("Cannot understand command: " + cmd.args[0]);
      }
    } else {
      metadata = false;
    }
    return metadata;
  }

}
