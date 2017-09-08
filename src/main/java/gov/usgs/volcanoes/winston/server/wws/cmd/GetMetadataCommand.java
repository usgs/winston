/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.winston.server.wws.cmd;

import java.util.List;
import java.util.Map;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.core.util.UtilException;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.Instrument;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.MalformedCommandException;
import gov.usgs.volcanoes.winston.server.wws.WinstonConsumer;
import gov.usgs.volcanoes.winston.server.wws.WwsBaseCommand;
import gov.usgs.volcanoes.winston.server.wws.WwsCommandString;
import io.netty.channel.ChannelHandlerContext;

/**
 * Return Channel details.
 * 
 * cmd = "GETMETADATA" <sp> <id> <sp> ( "INSTRUMENT" | "CHANNEL" )
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class GetMetadataCommand extends WwsBaseCommand {
  /**
   * Constructor.
   */
  public GetMetadataCommand() {
    super();
  }

  public void doCommand(ChannelHandlerContext ctx, WwsCommandString cmd)
      throws MalformedCommandException, UtilException {

    if (cmd.args == null || cmd.args.length != 1) {
      throw new MalformedCommandException();
    }

    StringBuilder sb = new StringBuilder();
    try {
      if ("INSTRUMENT".equals(cmd.args[0])) {
        List<Instrument> instruments = databasePool.doCommand(getInstrumentsConsumer());
        sb.append(String.format("%s %d\n", cmd.id, instruments.size()));
        sb.append(getInstrumentMetadata(instruments));

      } else if ("CHANNEL".equals(cmd.args[0])) {
        List<Channel> channels = databasePool.doCommand(getChannelsConsumer());
        sb.append(String.format("%s %d\n", cmd.id, channels.size()));
        sb.append(getChannelMetadata(channels));
      } else {
        throw new MalformedCommandException("Missing argument");
      }
    } catch (MalformedCommandException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new UtilException(ex.toString());
    }

    ctx.writeAndFlush(sb.toString());

  }

  private WinstonConsumer<List<Channel>> getChannelsConsumer() {
    return new WinstonConsumer<List<Channel>>() {

      public List<Channel> execute(WinstonDatabase winston) {
        return new Channels(winston).getChannels();
      }
    };
  }

  private WinstonConsumer<List<Instrument>> getInstrumentsConsumer() {
    return new WinstonConsumer<List<Instrument>>() {

      public List<Instrument> execute(WinstonDatabase winston) {
        return new Channels(winston).getInstruments();
      }
    };
  }

  private String getInstrumentMetadata(List<Instrument> insts) {
    final StringBuilder sb = new StringBuilder(insts.size() * 60);
    for (final Instrument inst : insts) {
      sb.append("name=");
      sb.append(escape(inst.name));
      sb.append(",");
      sb.append("description=");
      sb.append(escape(inst.description));
      sb.append(",");
      sb.append("longitude=");
      sb.append(inst.longitude);
      sb.append(",");
      sb.append("latitude=");
      sb.append(inst.latitude);
      sb.append(",");
      sb.append("height=");
      sb.append(inst.height);
      sb.append(",");
      sb.append("timezone=");
      sb.append(inst.timeZone);
      sb.append(",");
      appendMap(sb, inst.metadata);
      sb.append("\n");
    }
    return sb.toString();
  }

  private String getChannelMetadata(List<Channel> chs) {
    final StringBuilder sb = new StringBuilder(chs.size() * 60);
    for (final Channel ch : chs) {
      TimeSpan timeSpan = ch.timeSpan;
      sb.append("channel=");
      sb.append(ch.scnl.toString(" "));
      sb.append(",");
      sb.append("instrument=");
      sb.append(escape(ch.instrument.name));
      sb.append(",");
      sb.append("startTime=");
      sb.append(J2kSec.fromEpoch(timeSpan.startTime));
      sb.append(",");
      sb.append("endTime=");
      sb.append(J2kSec.fromEpoch(timeSpan.endTime));
      sb.append(",");
      sb.append("alias=");
      sb.append(escape(ch.alias));
      sb.append(",");
      sb.append("unit=");
      sb.append(escape(ch.unit));
      sb.append(",");
      sb.append("linearA=");
      sb.append(ch.linearA);
      sb.append(",");
      sb.append("linearB=");
      sb.append(ch.linearB);
      sb.append(",");
      appendList(sb, "groups", ch.groups);
      sb.append(",");
      appendMap(sb, ch.metadata);
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
