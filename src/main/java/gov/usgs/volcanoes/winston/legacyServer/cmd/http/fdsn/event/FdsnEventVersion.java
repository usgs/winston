package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.event;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.command.FdsnVersionCommand;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnEventVersion extends FdsnVersionCommand implements FdsnEventService {

  protected FdsnEventVersion(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    version = VERSION;
  }

  @Override
  public void sendResponse() {
    sendError(501, "Winston does not support the Event service");
  }

  @Override
  public String getCommand() {
    return "/fdsnws/event/1/version";
  }

}
