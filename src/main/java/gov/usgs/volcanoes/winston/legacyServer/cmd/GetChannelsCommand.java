package gov.usgs.volcanoes.winston.legacyServer.cmd;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.logging.Level;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.Channel;
import gov.usgs.volcanoes.winston.db.Channels;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;

/**
 * Example command string:
 * GETCHANNELS: GC METADATA
 *
 * @author Dan Cervelli
 */
public class GetChannelsCommand extends BaseCommand {
  private final Channels channels;

  public GetChannelsCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    channels = new Channels(db);
  }

  public void doCommand(final Object info, final SocketChannel channel) {
    final WWSCommandString cmd = new WWSCommandString((String) info);
    if (!cmd.isLegal(2) && !cmd.isLegal(3))
      return; // malformed command;

    boolean metadata = false;
    if (cmd.getString(2) != null && cmd.getString(2).equals("METADATA"))
      metadata = true;

    final List<Channel> chs = channels.getChannels();
    final StringBuilder sb = new StringBuilder(chs.size() * 60);
    sb.append(String.format("%s %d\n", cmd.getID(), chs.size()));
    for (final Channel ch : chs) {
      if (metadata)
        sb.append(ch.toMetadataString(maxDays) + "\n");
      else
        sb.append(ch.toPV2String(maxDays) + "\n");
    }

    netTools.writeString(sb.toString(), channel);
    String c = "GETCHANNELS";
    if (metadata)
      c = c + " (METADATA)";
    wws.log(Level.FINER, c + ".", channel);
  }
}
