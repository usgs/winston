package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.dataselect;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.command.FdsnWadlCommand;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnDataselectWadl extends FdsnWadlCommand implements FdsnDataselectService {

  public FdsnDataselectWadl(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    template = "www/fdsnws/dataselect_application.wadl";
  }

  @Override
  public String getCommand() {
    return "/fdsnws/dataselect/1/application.wadl";
  }
}
