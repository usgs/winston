package gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.dataselect;

import gov.usgs.net.NetTools;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.legacyServer.WWS;
import gov.usgs.volcanoes.winston.legacyServer.cmd.http.fdsn.command.FdsnUsageCommand;

/**
 *
 * @author Tom Parker
 *
 */
public class FdsnDataselectUsage extends FdsnUsageCommand implements FdsnDataselectService {

  public FdsnDataselectUsage(final NetTools nt, final WinstonDatabase db, final WWS wws) {
    super(nt, db, wws);
    UrlBuillderTemplate = "dataselect_UrlBuilder";
    InterfaceDescriptionTemplate = "dataselect_InterfaceDescription";
  }

  @Override
  public String getCommand() {
    return "/fdsnws/dataselect/1";
  }
}
