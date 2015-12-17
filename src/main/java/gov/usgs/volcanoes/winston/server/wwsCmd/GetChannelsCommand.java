/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wwsCmd;

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
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;
import gov.usgs.volcanoes.winston.legacyServer.cmd.BaseCommand;
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
      throws WwsMalformedCommand {

    if (!cmd.isLegal(2) && !cmd.isLegal(3)) {
      throw new WwsMalformedCommand("Malformed command");
    }

    boolean metadata = false;
    if ("METADATA".equals(cmd.getString(2))) {
      metadata = true;
    }

    WinstonDatabase winston = null;
    List<Channel> chs = null;
    try {
      winston = databasePool.borrowObject();
      if (!winston.checkConnect()) {
        LOGGER.error("WinstonDatabase unable to connect to MySQL.");
      } else {
        Channels channels = new Channels(winston);
        channels.setAparentRetention(maxDays * ONE_DAY_S); 
        chs = channels.getChannels(metadata);
        LOGGER.info("got {} channels", chs.size());
      }
    } catch (Exception e) {
      LOGGER.error("Unable to fulfill command.", e);
    } finally {
      if (winston != null) {
        databasePool.returnObject(winston);
      }
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
