package gov.usgs.volcanoes.winston.server.cmd.http.fdsn.station;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.server.WWS;
import gov.usgs.volcanoes.winston.server.cmd.http.fdsn.command.FdsnWadlCommand;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnStationWadl extends FdsnWadlCommand implements FdsnStationService {

  public FdsnStationWadl(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    template = "www/fdsnws/station_application.wadl";
    version = VERSION;
  }

  @Override
  public String getCommand() {
    return "/fdsnws/station/1/application.wadl";
  }

}
