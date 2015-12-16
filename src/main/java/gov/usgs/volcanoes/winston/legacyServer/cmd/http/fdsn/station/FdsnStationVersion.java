package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.station;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.command.FdsnVersionCommand;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnStationVersion extends FdsnVersionCommand implements FdsnStationService {

  public FdsnStationVersion(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    version = VERSION;
  }

  @Override
  public String getCommand() {
    return "/fdsnws/station/1/version";
  }
}
