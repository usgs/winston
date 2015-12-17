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
import java.util.Map;
import java.util.logging.Level;

import gov.usgs.net.ConnectionStatistics;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Instrument;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;
import gov.usgs.volcanoes.winston.legacyServer.cmd.BaseCommand;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * 
 * request = /^GETMETADATA:? \d( (INSTRUMENT|CHANNEL))?$/
 *
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetMetadataCommand extends WwsBaseCommand {
  private static final int PROTOCOL_VERSION = 3;

  private static final Logger LOGGER = LoggerFactory.getLogger(GetMetadataCommand.class);

  public GetMetadataCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws WwsMalformedCommand {
    final String[] ss = cmd.getCommandSplits();
    if (ss.length <= 2) {
      throw new WwsMalformedCommand("Malformed Command");
    }

    WinstonDatabase winston = null;
    String metadata = null;

    try {
      winston = databasePool.borrowObject();
      if (!winston.checkConnect()) {
        LOGGER.error("WinstonDatabase unable to connect to MySQL.");
      } else {
        Channels channels = new Channels(winston);
        if (ss[2].equals("INSTRUMENT")) {
          metadata = getInstrumentMetadata(cmd, channels);
        } else if (ss[2].equals("CHANNEL")) {
          metadata = getChannelMetadata(cmd, channels);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Unable to fulfill command.", e);
    } finally {
      if (winston != null) {
        databasePool.returnObject(winston);
      }
    }
    
    ctx.writeAndFlush(metadata);

  }

  private String getInstrumentMetadata(final WwsCommandString cmd, final Channels channels) {
    final List<Instrument> insts = channels.getInstruments();
    final StringBuilder sb = new StringBuilder(insts.size() * 60);
    sb.append(String.format("%s %d\n", cmd.getID(), insts.size()));
    for (final Instrument inst : insts) {
      sb.append("name=");
      sb.append(escape(inst.getName()));
      sb.append(",");
      sb.append("description=");
      sb.append(escape(inst.getDescription()));
      sb.append(",");
      sb.append("longitude=");
      sb.append(inst.getLongitude());
      sb.append(",");
      sb.append("latitude=");
      sb.append(inst.getLatitude());
      sb.append(",");
      sb.append("height=");
      sb.append(inst.getHeight());
      sb.append(",");
      sb.append("timezone=");
      sb.append(inst.getTimeZone());
      sb.append(",");
      appendMap(sb, inst.getMetadata());
      sb.append("\n");
    }
    return sb.toString();
  }

  private String getChannelMetadata(final WwsCommandString cmd, final Channels channels) {
    final List<Channel> chs = channels.getChannels(true);
    final StringBuilder sb = new StringBuilder(chs.size() * 60);
    sb.append(String.format("%s %d\n", cmd.getID(), chs.size()));
    for (final Channel ch : chs) {
      sb.append("channel=");
      sb.append(ch.getCode().replace('$', ' '));
      sb.append(",");
      sb.append("instrument=");
      sb.append(escape(ch.getInstrument().getName()));
      sb.append(",");
      sb.append("startTime=");
      sb.append(ch.getMinTime());
      sb.append(",");
      sb.append("endTime=");
      sb.append(ch.getMaxTime());
      sb.append(",");
      sb.append("alias=");
      sb.append(escape(ch.getAlias()));
      sb.append(",");
      sb.append("unit=");
      sb.append(escape(ch.getUnit()));
      sb.append(",");
      sb.append("linearA=");
      sb.append(ch.getLinearA());
      sb.append(",");
      sb.append("linearB=");
      sb.append(ch.getLinearB());
      sb.append(",");
      appendList(sb, "groups", ch.getGroups());
      sb.append(",");
      appendMap(sb, ch.getMetadata());
      sb.append("\n");
    }
    return sb.toString();
  }
  
  private String escape(final String s) {
    if (s == null)
      return "";
    return s.replaceAll(",", "\\\\c").replaceAll("\n", "\\\\n");
  }
  
  private void appendMap(final StringBuilder sb, final Map<String, String> map) {
    if (map == null)
      return;

    for (final String key : map.keySet()) {
      final String value = map.get(key);
      sb.append(escape(key));
      sb.append("=");
      sb.append(escape(value));
      sb.append(",");
    }
  }

  private void appendList(final StringBuilder sb, final String name, final List<String> list) {
    sb.append(name);
    sb.append("=");
    if (list == null)
      return;
    for (final String value : list) {
      sb.append(escape(value));
      sb.append("\\c");
    }
  }

}