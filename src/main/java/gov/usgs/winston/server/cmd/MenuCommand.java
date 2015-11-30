package gov.usgs.winston.server.cmd;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.WWSCommandString;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.logging.Level;

/**
 * example command: MENU -
 * 
 * @author Dan Cervelli
 */
public class MenuCommand extends BaseCommand {
	public MenuCommand(NetTools nt, WinstonDatabase db, WWS wws) {
		super(nt, db, wws);
	}

	public void doCommand(Object info, SocketChannel channel) {
		WWSCommandString cmd = new WWSCommandString((String) info);
		boolean scnl = false;
		if (cmd.length() == 3 && cmd.getString(2).equals("SCNL"))
			scnl = true;

		if (!scnl && !cmd.isLegal(2))
			return; // malformed command;

		StringBuffer sb = new StringBuffer(4096);
		sb.append(cmd.getID() + " ");
		List<String> menu = emulator.getWaveServerMenu(scnl, 0, 0, maxDays);
		for (String s : menu)
			sb.append(s);
		sb.append('\n');

		netTools.writeString(sb.toString(), channel);
		wws.log(Level.FINER, "MENU" + (scnl ? " (SCNL)" : "") + ".", channel);
	}
}
