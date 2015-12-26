/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import gov.usgs.net.ConnectionStatistics;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;
import gov.usgs.volcanoes.winston.legacyServer.cmd.BaseCommand;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * 
 * request = /^GETCHANNELS:? GC( METADATA)?$/
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetChannelsCommand extends WwsBaseCommand {
  private static final int PROTOCOL_VERSION = 3;

  private static final Logger LOGGER = LoggerFactory.getLogger(GetChannelsCommand.class);

  public GetChannelsCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    if (!cmd.isLegal(2) && !cmd.isLegal(3)) {
      throw new MalformedCommandException();
    }

    final boolean metadata;
    if ("METADATA".equals(cmd.getString(2))) {
      metadata = true;
    } else {
      metadata = false;
    }

    List<Channel> chs = null;
    try {
      chs = databasePool.doCommand(new WinstonConsumer<List<Channel>>() {
        public List<Channel> execute(WinstonDatabase winston) {
          Channels channels = new Channels(winston);
          channels.setAparentRetention(maxDays * ONE_DAY_S); 
          return channels.getChannels(metadata);
        }
      });
    } catch (Exception e) {
      throw new UtilException("Unable to get channels.");
    }

    final StringBuilder sb = new StringBuilder(chs.size() * 60);
    sb.append(String.format("%s %d\n", cmd.getID(), chs.size()));
    for (final Channel ch : chs) {
      if (metadata)
        sb.append(ch.toMetadataString() + "\n");
      else
        sb.append(ch.toPV2String() + "\n");
    }
    LOGGER.info("maxDays = {}", maxDays);
    ctx.writeAndFlush(sb.toString());
  }
}
