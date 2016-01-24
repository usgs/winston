package gov.usgs.volcanoes.winston.legacyServer.cmd;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.logging.Level;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.WWSCommandString;

/**
 * example command: MENU -
 *
 * @author Dan Cervelli
 */
public class MenuCommand extends BaseCommand {
  public MenuCommand(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
  }

  public void doCommand(final Object info, final SocketChannel channel) {
    final WWSCommandString cmd = new WWSCommandString((String) info);
    boolean scnl = false;
    if (cmd.length() == 3 && cmd.getString(2).equals("SCNL"))
      scnl = true;

    if (!scnl && !cmd.isLegal(2))
      return; // malformed command;

    final StringBuffer sb = new StringBuffer(4096);
    sb.append(cmd.getID() + " ");
    final List<String> menu = emulator.getWaveServerMenu(scnl, 0, 0, maxDays);
    for (final String s : menu)
      sb.append(s);
    sb.append('\n');

    netTools.writeString(sb.toString(), channel);
    wws.log(Level.FINER, "MENU" + (scnl ? " (SCNL)" : "") + ".", channel);
  }
}
