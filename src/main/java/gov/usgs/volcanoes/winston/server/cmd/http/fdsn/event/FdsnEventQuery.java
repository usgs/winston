package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.event;

import gov.usgs.net.NetTools;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.server.WWS;
import gov.usgs.winston.server.cmd.http.fdsn.command.FdsnQueryCommand;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnEventQuery extends FdsnQueryCommand implements FdsnEventService {

  public FdsnEventQuery(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    version = VERSION;
  }

  @Override
  public void sendResponse() {
    sendError(501, "Winston does not support the Event service");
  }

  @Override
  public String getCommand() {
    return "/fdsnws/event/1/query";
  }
}
