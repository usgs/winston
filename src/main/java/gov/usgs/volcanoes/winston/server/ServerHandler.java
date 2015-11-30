package gov.usgs.volcanoes.winston.server;

import java.nio.channels.SocketChannel;

import gov.usgs.net.CommandHandler;
import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.cmd.BaseCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetChannelsCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetMetadataCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetSCNCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetSCNLCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetSCNLHeliRawCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetSCNLRSAMRawCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetSCNLRawCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetSCNRawCommand;
import gov.usgs.volcanoes.winston.server.cmd.GetWaveRawCommand;
import gov.usgs.volcanoes.winston.server.cmd.HttpCommand;
import gov.usgs.volcanoes.winston.server.cmd.MenuCommand;
import gov.usgs.volcanoes.winston.server.cmd.StatusCommand;

/**
 * A class that handles WWS requests.
 *
 * TODO: send ERROR REQUEST on bad commands.
 * TODO: compressed wave protocol.
 * 
 * FYI, known bugs in wave_serverV that this program mimics:<br>
 * -- SCN not found on MENUSCN replies with extra request ID<br>
 * -- MENUSCN does not append \n properly<br>
 * 
 *
 * @author Dan Cervelli
 */
public class ServerHandler extends CommandHandler
{
	private static final int PROTOCOL_VERSION = 3;
	
	private WinstonDatabase winston;

	private static int instances = 0;
	private WWS wws;
	private NetTools netTools;

	public ServerHandler(WWS s)
	{
		super(s, "WWSHandler-" + instances++);
		
		wws = (WWS)s;
		netTools = new NetTools();
		netTools.setServer(wws);
		winston = new WinstonDatabase(wws.getWinstonDriver(), wws.getWinstonURL(), wws.getWinstonPrefix(), wws.getWinstonStatementCacheCap());

		slowCommandTime = wws.slowCommandTime;
		setupCommandHandlers();
	}

	protected void setupCommandHandlers()
	{
		addCommand("VERSION", new BaseCommand(netTools, winston, wws)
				{
					public void doCommand(Object info, SocketChannel channel)
					{
						netTools.writeString("PROTOCOL_VERSION: " + PROTOCOL_VERSION + "\n", channel);
					}
				});
		addCommand("MENU", new MenuCommand(netTools, winston, wws));
		addCommand("STATUS", new StatusCommand(netTools, winston, wws));
		addCommand("GETSCNRAW", new GetSCNRawCommand(netTools, winston, wws));
		addCommand("GETSCNLRAW", new GetSCNLRawCommand(netTools, winston, wws));
		addCommand("GETSCN", new GetSCNCommand(netTools, winston, wws));
		addCommand("GETSCNL", new GetSCNLCommand(netTools, winston, wws));
		addCommand("GETSCNLHELIRAW", new GetSCNLHeliRawCommand(netTools, winston, wws));
		addCommand("GETSCNLRSAMRAW", new GetSCNLRSAMRawCommand(netTools, winston, wws));
		addCommand("GETCHANNELS", new GetChannelsCommand(netTools, winston, wws));
		addCommand("GETWAVERAW", new GetWaveRawCommand(netTools, winston, wws));
		addCommand("GETMETADATA", new GetMetadataCommand(netTools, winston, wws));
		if (wws.isHttpAllowed()) {
			addCommand("GET", new HttpCommand(netTools, winston, wws, this));
            addCommand("POST", new HttpCommand(netTools, winston, wws, this));
		}
	}
}